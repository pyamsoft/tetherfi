package com.pyamsoft.widefi.server.widi

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pGroup
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Looper
import androidx.annotation.CheckResult
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import com.pyamsoft.pydroid.core.Enforcer
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.widefi.server.*
import com.pyamsoft.widefi.server.event.ConnectionEvent
import com.pyamsoft.widefi.server.event.ErrorEvent
import com.pyamsoft.widefi.server.permission.PermissionGuard
import com.pyamsoft.widefi.server.proxy.SharedProxy
import com.pyamsoft.widefi.server.status.RunningStatus
import com.pyamsoft.widefi.server.widi.receiver.WiDiReceiver
import com.pyamsoft.widefi.server.widi.receiver.WidiNetworkEvent
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

@Singleton
internal class WifiDirectWiDiNetwork
@Inject
internal constructor(
    private val preferences: ServerPreferences,
    private val context: Context,
    private val permissionGuard: PermissionGuard,
    @ServerInternalApi private val proxy: SharedProxy,
    @ServerInternalApi private val receiver: WiDiReceiver,
    @ServerInternalApi private val dispatcher: CoroutineDispatcher,
    status: WiDiStatus,
) : BaseServer(status), WiDiNetwork {

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

  @CheckResult
  @RequiresApi(Build.VERSION_CODES.Q)
  private suspend fun getPreferredSsid(): String {
    return preferences.getSsid()
  }

  @CheckResult
  @RequiresApi(Build.VERSION_CODES.Q)
  private suspend fun getPreferredPassword(): String {
    return preferences.getPassword()
  }

  @CheckResult
  @RequiresApi(Build.VERSION_CODES.Q)
  private suspend fun getPreferredBand(): Int {
    return when (preferences.getNetworkBand()) {
      ServerNetworkBand.AUTO -> WifiP2pConfig.GROUP_OWNER_BAND_AUTO
      ServerNetworkBand.LEGACY -> WifiP2pConfig.GROUP_OWNER_BAND_2GHZ
      ServerNetworkBand.MODERN -> WifiP2pConfig.GROUP_OWNER_BAND_5GHZ
    }
  }

  @CheckResult
  private suspend fun getConfiguration(): WifiP2pConfig? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      WifiP2pConfig.Builder()
          .setNetworkName(ServerDefaults.asSsid(getPreferredSsid()))
          .setPassphrase(getPreferredPassword())
          .setGroupOperatingBand(getPreferredBand())
          .build()
    } else {
      null
    }
  }

  @SuppressLint("MissingPermission")
  private fun createGroupQ(
      channel: WifiP2pManager.Channel,
      config: WifiP2pConfig,
      listener: WifiP2pManager.ActionListener,
  ) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
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

        val config = getConfiguration()

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

          if (config != null) {
            createGroupQ(channel, config, listener)
          } else {
            wifiP2PManager.createGroup(
                channel,
                listener,
            )
          }
        }
      }

  @CheckResult
  private suspend fun removeGroup(channel: WifiP2pManager.Channel): RunningStatus =
      withContext(context = Dispatchers.Main) {
        Timber.d("Stop existing network")

        return@withContext suspendCoroutine { cont ->
          wifiP2PManager.removeGroup(
              channel,
              object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                  Timber.d("Wifi P2P Channel is removed")
                  closeSilent(channel)
                  cont.resume(RunningStatus.NotRunning)
                }

                override fun onFailure(reason: Int) {
                  val msg = "Failed to stop network: ${reasonToString(reason)}"
                  Timber.w(msg)
                  cont.resume(RunningStatus.Error(msg))
                }
              },
          )
        }
      }

  private suspend fun startNetwork(onStart: () -> Unit) =
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

            Timber.d("Register wifi receiver")
            receiver.register()
          }

          Timber.d("Network started, start proxy")
          proxy.start()
          Timber.d("Proxy started!")
          onStart()
        } else {
          Timber.w("Group failed creation, stop proxy")
          proxy.stop()
          Timber.d("Proxy stopped!")
        }

        status.set(runningStatus)
      }

  private suspend fun stopNetwork(onStop: () -> Unit = {}) {
    Enforcer.assertOffMainThread()

    val channel = getChannel()

    if (channel == null) {
      status.set(RunningStatus.NotRunning)
      onStop()
      return
    }

    Timber.d("Shutting down wifi network")
    status.set(RunningStatus.Stopping)

    val runningStatus = removeGroup(channel)
    if (runningStatus == RunningStatus.NotRunning) {
      mutex.withLock {
        Timber.d("Clear wifi channel")
        wifiChannel = null

        Timber.d("Unregister wifi receiver")
        receiver.unregister()
      }
      Timber.d("Stop proxy when wifi network removed")
      proxy.stop()
    } else {
      Timber.w("Group failed removal, stop proxy anyway")
      proxy.stop()
    }
    status.set(runningStatus)
    onStop()
  }

  @CheckResult
  private fun asWifiBand(group: WifiP2pGroup): ServerNetworkBand {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      when (group.frequency) {
        WifiP2pConfig.GROUP_OWNER_BAND_5GHZ -> ServerNetworkBand.MODERN
        WifiP2pConfig.GROUP_OWNER_BAND_2GHZ -> ServerNetworkBand.LEGACY
        WifiP2pConfig.GROUP_OWNER_BAND_AUTO -> ServerNetworkBand.AUTO
        else -> ServerNetworkBand.AUTO
      }
    } else {
      ServerNetworkBand.AUTO
    }
  }

  @CheckResult
  @SuppressLint("MissingPermission")
  private suspend fun resolveCurrentGroup(channel: WifiP2pManager.Channel): WiDiNetwork.GroupInfo? =
      withContext(context = Dispatchers.Main) {
        return@withContext suspendCoroutine { cont ->
          Timber.d("Get current group info")

          wifiP2PManager.requestGroupInfo(
              channel,
          ) { group ->
            if (group == null) {
              Timber.w("Group info was null from P2PManager")
              cont.resume(null)
            } else {
              cont.resume(
                  WiDiNetwork.GroupInfo(
                      ssid = group.networkName,
                      password = group.passphrase,
                      band = asWifiBand(group),
                  ),
              )
            }
          }
        }
      }

  @CheckResult
  private suspend fun resolveConnectionInfo(
      channel: WifiP2pManager.Channel
  ): WiDiNetwork.ConnectionInfo? =
      withContext(context = Dispatchers.Main) {
        return@withContext suspendCoroutine { cont ->
          wifiP2PManager.requestConnectionInfo(
              channel,
          ) { conn ->
            if (conn == null) {
              Timber.w("Connection Info info was null from P2PManager")
              cont.resume(null)
            } else {
              cont.resume(
                  WiDiNetwork.ConnectionInfo(
                      ip = conn.groupOwnerAddress?.hostAddress ?: "No IP Address",
                      hostName = conn.groupOwnerAddress?.hostName ?: "No Host Name",
                  ),
              )
            }
          }
        }
      }

  override suspend fun onErrorEvent(block: (ErrorEvent) -> Unit) {
    return proxy.onErrorEvent(block)
  }

  override suspend fun onConnectionEvent(block: (ConnectionEvent) -> Unit) {
    return proxy.onConnectionEvent(block)
  }

  override suspend fun getGroupInfo(): WiDiNetwork.GroupInfo? =
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

  override suspend fun getConnectionInfo(): WiDiNetwork.ConnectionInfo? =
      withContext(context = Dispatchers.Main) {
        val channel = getChannel()
        if (channel == null) {
          Timber.w("Cannot get connection info without Wifi channel")
          return@withContext null
        }

        return@withContext resolveConnectionInfo(channel)
      }

  override fun start(onStart: () -> Unit) {
    scope.launch(context = dispatcher) {
      Enforcer.assertOffMainThread()

      try {
        stopNetwork()
        startNetwork(onStart)
      } catch (e: Throwable) {
        Timber.e(e, "Error starting Network")
        status.set(RunningStatus.Error(e.message ?: "An error occurred while starting the Network"))
      }
    }
  }

  override fun stop(onStop: () -> Unit) {
    scope.launch(context = dispatcher) {
      Enforcer.assertOffMainThread()

      try {
        stopNetwork(onStop)
      } catch (e: Throwable) {
        Timber.e(e, "Error stopping Network")
        status.set(RunningStatus.Error(e.message ?: "An error occurred while stopping the Network"))
      }
    }
  }

  override suspend fun onWifiDirectEvent(block: (WidiNetworkEvent) -> Unit) {
    return receiver.onEvent { block(it) }
  }

  override suspend fun onProxyStatusChanged(block: (RunningStatus) -> Unit) {
    return proxy.onStatusChanged(block)
  }

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
