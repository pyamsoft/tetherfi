package com.pyamsoft.tetherfi.server.widi

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.Channel
import android.os.Build
import android.os.Looper
import androidx.annotation.CheckResult
import androidx.core.content.getSystemService
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.core.Enforcer
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.tetherfi.server.BaseServer
import com.pyamsoft.tetherfi.server.ServerDefaults
import com.pyamsoft.tetherfi.server.event.ServerShutdownEvent
import com.pyamsoft.tetherfi.server.permission.PermissionGuard
import com.pyamsoft.tetherfi.server.status.RunningStatus
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber

internal abstract class WifiDirectNetwork
protected constructor(
    private val shutdownBus: EventBus<ServerShutdownEvent>,
    private val context: Context,
    private val permissionGuard: PermissionGuard,
    private val dispatcher: CoroutineDispatcher,
    private val config: WiDiConfig,
    status: WiDiStatus,
) : BaseServer(status), WiDiNetwork, WiDiNetworkStatus {

  private val wifiP2PManager by lazy {
    context.applicationContext.getSystemService<WifiP2pManager>().requireNotNull()
  }

  private val scope by lazy { CoroutineScope(context = dispatcher) }

  private val mutex = Mutex()
  private var wifiChannel: Channel? = null

  @CheckResult
  private fun createChannel(): Channel? {
    Timber.d("Creating WifiP2PManager Channel")

    // This can return null if initialization fails
    return wifiP2PManager.initialize(
        context.applicationContext,
        Looper.getMainLooper(),
    ) {
      scope.launch(context = dispatcher) {
        Timber.d("WifiP2PManager Channel died. Kill network")
        stopNetwork(resetStatus = false)
      }
    }
  }

  @CheckResult
  private suspend fun getChannel(): Channel? {
    return mutex.withLock { wifiChannel }
  }

  @SuppressLint("MissingPermission")
  private fun createGroupQ(
      channel: Channel,
      config: WifiP2pConfig,
      listener: WifiP2pManager.ActionListener,
  ) {
    if (ServerDefaults.canUseCustomConfig()) {
      wifiP2PManager.createGroup(
          channel,
          config,
          listener,
      )
    } else {
      throw IllegalStateException("Called createGroupQ but not Q: ${Build.VERSION.SDK_INT}")
    }
  }

  @SuppressLint("MissingPermission")
  private suspend fun createGroup(channel: Channel): RunningStatus =
      withContext(context = Dispatchers.Main) {
        Timber.d("Creating new wifi p2p group")

        val conf = config.getConfiguration()

        return@withContext suspendCoroutine { cont ->
          val listener =
              object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                  Timber.d("New network created")
                  cont.resume(RunningStatus.Running)
                }

                override fun onFailure(reason: Int) {
                  val msg = "Failed to create network: ${reasonToString(reason)}"
                  Timber.w(msg)
                  cont.resume(RunningStatus.Error(msg))
                }
              }

          if (conf != null) {
            createGroupQ(channel, conf, listener)
          } else {
            wifiP2PManager.createGroup(
                channel,
                listener,
            )
          }
        }
      }

  @CheckResult
  private suspend fun removeGroup(channel: Channel): Unit =
      withContext(context = Dispatchers.Main) {
        // Close the Group here
        Timber.d("Stop existing WiFi Group")
        return@withContext suspendCoroutine { cont ->
          wifiP2PManager.removeGroup(
              channel,
              object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                  Timber.d("Wifi P2P Channel is removed")
                  cont.resume(Unit)
                }

                override fun onFailure(reason: Int) {
                  Timber.w("Failed to stop network: ${reasonToString(reason)}")

                  Timber.d("Close Group failed but continue teardown anyway")
                  cont.resume(Unit)
                }
              },
          )
        }
      }

  private suspend fun startNetwork() =
      withContext(context = dispatcher) {
        Enforcer.assertOffMainThread()

        if (!permissionGuard.canCreateWiDiNetwork()) {
          Timber.w("Missing permissions for making WiDi network")
          status.set(RunningStatus.NotRunning)
          return@withContext
        }

        Timber.d("Start new network")
        status.set(RunningStatus.Starting)

        val channel = createChannel()
        if (channel == null) {
          Timber.w("Failed to create channel, cannot initialize WiDi network")

          completeStop { status.set(RunningStatus.Error("Failed to create Wi-Fi Direct Channel")) }
          return@withContext
        }

        val runningStatus = createGroup(channel)
        if (runningStatus == RunningStatus.Running) {
          Timber.d("Network started")

          // Only store the channel if it successfully "finished" creating.
          mutex.withLock {
            Timber.d("Store WiFi channel")
            wifiChannel = channel
          }

          onNetworkStarted()
        } else {
          Timber.w("Group failed creation, stop proxy")

          // Remove whatever was created (should be a no-op if everyone follows API correctly)
          shutdownWifiNetwork(channel)

          completeStop { Timber.w("Stopping proxy after Group failed to create") }
        }

        status.set(runningStatus)
      }

  private suspend fun completeStop(onStop: () -> Unit) {
    onNetworkStopped()
    onStop()
  }

  // Lock the mutex to avoid anyone else from using the channel during closing
  private suspend fun shutdownWifiNetwork(channel: Channel) =
      mutex.withLock {
        // This may fail if WiFi is off, but thats fine since if WiFi is off,
        // the system has already cleaned us up.
        removeGroup(channel)

        // Close the wifi channel now that we are done with it
        Timber.d("Close WiFiP2PManager channel")
        closeSilent(channel)

        // Clear out so nobody else can use a dead channel
        wifiChannel = null
      }

  private suspend fun stopNetwork(resetStatus: Boolean) {
    Enforcer.assertOffMainThread()

    val channel = getChannel()

    // If we have no channel, we haven't started yet. Make sure we are clean, but shi
    // is basically a no-op
    if (channel == null) {
      completeStop {
        if (resetStatus) {
          Timber.d("Resetting status back to not running")
          status.set(RunningStatus.NotRunning)
        }
      }
      return
    }

    // If we do have a channel, mark shutting down as we clean up
    Timber.d("Shutting down wifi network")
    status.set(RunningStatus.Stopping)

    shutdownWifiNetwork(channel)

    completeStop {
      Timber.d("Proxy was stopped")
      status.set(RunningStatus.NotRunning)
    }
  }

  @CheckResult
  @SuppressLint("MissingPermission")
  private suspend fun resolveCurrentGroup(channel: Channel): WiDiNetworkStatus.GroupInfo? =
      withContext(context = Dispatchers.Main) {
        return@withContext suspendCoroutine { cont ->
          wifiP2PManager.requestGroupInfo(channel) { group ->
            if (group == null) {
              Timber.w("No WiFi Direct Group info available")
              cont.resume(null)
            } else {
              cont.resume(
                  WiDiNetworkStatus.GroupInfo(
                      ssid = group.networkName,
                      password = group.passphrase,
                  ),
              )
            }
          }
        }
      }

  @CheckResult
  private suspend fun resolveConnectionInfo(channel: Channel): WiDiNetworkStatus.ConnectionInfo? =
      withContext(context = Dispatchers.Main) {
        return@withContext suspendCoroutine { cont ->
          wifiP2PManager.requestConnectionInfo(channel) { conn ->
            if (conn == null) {
              Timber.w("No WiFi Direct Connection info available")
              cont.resume(null)
            } else {
              cont.resume(
                  WiDiNetworkStatus.ConnectionInfo(
                      ip = conn.groupOwnerAddress?.hostAddress ?: "No IP Address",
                      hostName = conn.groupOwnerAddress?.hostName ?: "No Host Name",
                  ),
              )
            }
          }
        }
      }

  final override suspend fun getGroupInfo(): WiDiNetworkStatus.GroupInfo? =
      withContext(context = Dispatchers.Main) {
        if (!permissionGuard.canCreateWiDiNetwork()) {
          Timber.w("Missing permissions, cannot get Group Info")
          return@withContext null
        }

        val channel = getChannel()
        if (channel == null) {
          Timber.w("Cannot get group info without Wifi channel")
          return@withContext null
        }

        return@withContext resolveCurrentGroup(channel)
      }

  final override suspend fun getConnectionInfo(): WiDiNetworkStatus.ConnectionInfo? =
      withContext(context = Dispatchers.Main) {
        if (!permissionGuard.canCreateWiDiNetwork()) {
          Timber.w("Missing permissions, cannot get Connection Info")
          return@withContext null
        }

        val channel = getChannel()
        if (channel == null) {
          Timber.w("Cannot get connection info without Wifi channel")
          return@withContext null
        }

        return@withContext resolveConnectionInfo(channel)
      }

  final override fun start() {
    scope.launch(context = dispatcher) {
      Enforcer.assertOffMainThread()

      Timber.d("Starting Wi-Fi Direct Network...")
      try {
        stopNetwork(resetStatus = true)
        startNetwork()
      } catch (e: Throwable) {
        Timber.e(e, "Error starting Network")
        status.set(RunningStatus.Error(e.message ?: "An error occurred while starting the Network"))
      }
    }
  }

  final override fun stop() {
    scope.launch(context = dispatcher) {
      Enforcer.assertOffMainThread()

      Timber.d("Stopping Wi-Fi Direct Network...")
      try {
        stopNetwork(resetStatus = true)
      } catch (e: Throwable) {
        Timber.e(e, "Error stopping Network")
        status.set(RunningStatus.Error(e.message ?: "An error occurred while stopping the Network"))
      } finally {
        // Fire the shutdown event to the service
        Timber.d("Fire final shutdown event.")
        shutdownBus.send(ServerShutdownEvent)

        Timber.d("Wi-Fi Direct network is shutdown")
      }
    }
  }

  protected abstract suspend fun onNetworkStarted()

  protected abstract suspend fun onNetworkStopped()

  companion object {

    @JvmStatic
    @CheckResult
    private fun reasonToString(reason: Int): String {
      return when (reason) {
        WifiP2pManager.P2P_UNSUPPORTED -> "P2P Unsupported"
        WifiP2pManager.NO_SERVICE_REQUESTS -> "No Service Requests"
        WifiP2pManager.ERROR -> "Error"
        WifiP2pManager.BUSY -> "Busy"
        else -> "Unknown"
      }
    }

    @JvmStatic
    private fun closeSilent(s: Channel) {
      try {
        s.close()
      } catch (e: Throwable) {
        Timber.e(e, "Failed to close WifiP2P Channel")
      }
    }
  }
}
