package com.pyamsoft.tetherfi.server.widi

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Looper
import androidx.annotation.CheckResult
import androidx.core.content.getSystemService
import com.pyamsoft.pydroid.core.Enforcer
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.tetherfi.server.BaseServer
import com.pyamsoft.tetherfi.server.ServerDefaults
import com.pyamsoft.tetherfi.server.permission.PermissionGuard
import com.pyamsoft.tetherfi.server.status.RunningStatus
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
  private var wifiChannel: WifiP2pManager.Channel? = null

  @CheckResult
  private fun createChannel(): WifiP2pManager.Channel? {
    return wifiP2PManager.initialize(context.applicationContext, Looper.getMainLooper(), null)
  }

  @CheckResult
  private suspend fun getChannel(): WifiP2pManager.Channel? {
    return mutex.withLock { wifiChannel }
  }

  @SuppressLint("MissingPermission")
  private fun createGroupQ(
      channel: WifiP2pManager.Channel,
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
  private suspend fun createGroup(channel: WifiP2pManager.Channel): RunningStatus =
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
  private suspend fun removeGroup(channel: WifiP2pManager.Channel): Unit =
      withContext(context = Dispatchers.Main) {
        Timber.d("Stop existing network")

        return@withContext suspendCoroutine { cont ->
          wifiP2PManager.removeGroup(
              channel,
              object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                  Timber.d("Wifi P2P Channel is removed")
                  closeSilent(channel)
                  cont.resume(Unit)
                }

                override fun onFailure(reason: Int) {
                  Timber.w("Failed to stop network: ${reasonToString(reason)}")

                  Timber.d("Close Group failed but continue teardown anyway")
                  closeSilent(channel)
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

        val channel = createChannel()
        if (channel == null) {
          Timber.w("Failed to create channel, cannot initialize WiDi network")
          status.set(RunningStatus.NotRunning)
          return@withContext
        }

        Timber.d("Start new network")
        status.set(RunningStatus.Starting)

        val runningStatus = createGroup(channel)
        if (runningStatus == RunningStatus.Running) {
          mutex.withLock {
            Timber.d("Store WiFi channel")
            wifiChannel = channel
          }

          Timber.d("Network started")
          onNetworkStarted()
        } else {
          Timber.w("Group failed creation, stop proxy")
          completeStop { Timber.w("Stopped proxy because group failed creation") }
        }

        status.set(runningStatus)
      }

  private suspend fun completeStop(onStop: () -> Unit) {
    Timber.d("Stop when wifi network removed")
    onNetworkStopped()

    onStop()
  }

  private suspend fun stopNetwork() {
    Enforcer.assertOffMainThread()

    val channel = getChannel()

    if (channel == null) {
      completeStop { status.set(RunningStatus.NotRunning) }
      return
    }

    Timber.d("Shutting down wifi network")
    status.set(RunningStatus.Stopping)

    // This may fail if WiFi is off, but thats fine since if WiFi is off, the system has already
    // cleaned us up.
    removeGroup(channel)

    mutex.withLock {
      Timber.d("Clear wifi channel")
      wifiChannel = null
    }

    completeStop {
      Timber.d("Proxy was stopped")
      status.set(RunningStatus.NotRunning)
    }
  }

  @CheckResult
  @SuppressLint("MissingPermission")
  private suspend fun resolveCurrentGroup(
      channel: WifiP2pManager.Channel
  ): WiDiNetworkStatus.GroupInfo? =
      withContext(context = Dispatchers.Main) {
        return@withContext suspendCoroutine { cont ->
          wifiP2PManager.requestGroupInfo(
              channel,
          ) { group ->
            if (group == null) {
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
  private suspend fun resolveConnectionInfo(
      channel: WifiP2pManager.Channel
  ): WiDiNetworkStatus.ConnectionInfo? =
      withContext(context = Dispatchers.Main) {
        return@withContext suspendCoroutine { cont ->
          wifiP2PManager.requestConnectionInfo(
              channel,
          ) { conn ->
            if (conn == null) {
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
          Timber.w("Missing permissions for making WiDi network")
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
        stopNetwork()
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
        stopNetwork()
      } catch (e: Throwable) {
        Timber.e(e, "Error stopping Network")
        status.set(RunningStatus.Error(e.message ?: "An error occurred while stopping the Network"))
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
    private fun closeSilent(s: WifiP2pManager.Channel) {
      try {
        s.close()
      } catch (e: Throwable) {
        Timber.e(e, "Failed to close WifiP2P Channel")
      }
    }
  }
}
