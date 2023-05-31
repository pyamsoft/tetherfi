/*
 * Copyright 2023 pyamsoft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pyamsoft.tetherfi.server.widi

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pGroup
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.Channel
import android.os.Build
import android.os.Looper
import androidx.annotation.CheckResult
import androidx.core.content.getSystemService
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.core.ThreadEnforcer
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.pydroid.util.ifNotCancellation
import com.pyamsoft.tetherfi.core.AppDevEnvironment
import com.pyamsoft.tetherfi.server.BaseServer
import com.pyamsoft.tetherfi.server.ServerDefaults
import com.pyamsoft.tetherfi.server.event.ServerShutdownEvent
import com.pyamsoft.tetherfi.server.permission.PermissionGuard
import com.pyamsoft.tetherfi.server.status.RunningStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

internal abstract class WifiDirectNetwork
protected constructor(
    private val shutdownBus: EventBus<ServerShutdownEvent>,
    private val appContext: Context,
    private val permissionGuard: PermissionGuard,
    private val config: WiDiConfig,
    private val appEnvironment: AppDevEnvironment,
    private val enforcer: ThreadEnforcer,
    status: WiDiStatus,
) : BaseServer(status), WiDiNetwork, WiDiNetworkStatus {

  private val wifiP2PManager by lazy {
    appContext.getSystemService<WifiP2pManager>().requireNotNull()
  }

  private val groupInfoChannel =
      MutableStateFlow<WiDiNetworkStatus.GroupInfo>(WiDiNetworkStatus.GroupInfo.Empty)
  private val connectionInfoChannel =
      MutableStateFlow<WiDiNetworkStatus.ConnectionInfo>(WiDiNetworkStatus.ConnectionInfo.Empty)

  private val mutex = Mutex()
  private var wifiChannel: Channel? = null

  @CheckResult
  private fun createChannel(): Channel? {
    enforcer.assertOffMainThread()

    Timber.d("Creating WifiP2PManager Channel")

    // This can return null if initialization fails
    return wifiP2PManager.initialize(
        appContext,
        Looper.getMainLooper(),
    ) {
      // Called on the Main thread when the WifiP2p loses connection.
      // In this case the channel may already be dead.
      //
      // We may not be able to perform a full clean stop.
      // Use Dispatchers.Default here instead of ProxyDispatcher since this can run outside of
      // Server cycle
      CoroutineScope(context = Dispatchers.Default).launch {
        Timber.d("WifiP2PManager Channel died. Kill network")
        // Fire the shutdown event to the service
        //
        // The service shutdown will properly clean up things like this WDN, as well as wakelocks
        // and notifications
        shutdownBus.emit(ServerShutdownEvent)
      }
    }
  }

  @CheckResult
  private suspend fun getChannel(): Channel? {
    enforcer.assertOffMainThread()

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
  private suspend fun createGroup(channel: Channel): RunningStatus {
    enforcer.assertOffMainThread()

    Timber.d("Creating new wifi p2p group")

    val conf = config.getConfiguration()

    return suspendCoroutine { cont ->
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
  private suspend fun removeGroup(channel: Channel) {
    enforcer.assertOffMainThread()

    Timber.d("Stop existing WiFi Group")
    suspendCoroutine { cont ->
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
      withContext(context = Dispatchers.Default) {
        enforcer.assertOffMainThread()

        if (!permissionGuard.canCreateWiDiNetwork()) {
          Timber.w("Missing permissions for making WiDi network")
          status.set(RunningStatus.NotRunning)
          return@withContext
        }

        Timber.d("Start new network")
        status.set(RunningStatus.Starting, clearError = true)
        val channel = createChannel()

        if (channel == null) {
          Timber.w("Failed to create channel, cannot initialize WiDi network")

          completeStop(this, clearErrorStatus = false) {
            status.set(RunningStatus.Error("Failed to create Wi-Fi Direct Channel"))
          }
          return@withContext
        }

        val runningStatus = createGroup(channel)
        if (runningStatus is RunningStatus.Running) {
          Timber.d("Network started")

          // Only store the channel if it successfully "finished" creating.
          mutex.withLock {
            Timber.d("Store WiFi channel")
            wifiChannel = channel
          }

          updateNetworkInfoChannels()

          launch(context = Dispatchers.Default) { onNetworkStarted() }

          Timber.d("WiDi network has started: $runningStatus")
          status.set(runningStatus)
        } else {
          Timber.w("Group failed creation, stop proxy")

          // Remove whatever was created (should be a no-op if everyone follows API correctly)
          shutdownWifiNetwork(channel)

          completeStop(this, clearErrorStatus = false) {
            Timber.w("Stopping proxy after Group failed to create")
            status.set(runningStatus)
          }
        }
      }

  private suspend fun completeStop(
      scope: CoroutineScope,
      clearErrorStatus: Boolean,
      onStop: () -> Unit,
  ) {
    enforcer.assertOffMainThread()

    updateNetworkInfoChannels()

    scope.launch(context = Dispatchers.Default) { onNetworkStopped(clearErrorStatus) }

    onStop()
  }

  // Lock the mutex to avoid anyone else from using the channel during closing
  private suspend fun shutdownWifiNetwork(channel: Channel) {
    enforcer.assertOffMainThread()

    mutex.withLock {
      // This may fail if WiFi is off, but that's fine since if WiFi is off,
      // the system has already cleaned us up.
      removeGroup(channel)

      // Close the wifi channel now that we are done with it
      Timber.d("Close WiFiP2PManager channel")
      closeSilent(channel)

      // Clear out so nobody else can use a dead channel
      wifiChannel = null
    }
  }

  private suspend fun stopNetwork(clearErrorStatus: Boolean) =
      withContext(context = Dispatchers.Default) {
        enforcer.assertOffMainThread()

        val channel = getChannel()

        // If we have no channel, we haven't started yet. Make sure we are clean, but this
        // is basically a no-op
        if (channel == null) {
          completeStop(this, clearErrorStatus) {
            Timber.d("Resetting status back to not running")
            status.set(
                RunningStatus.NotRunning,
                clearError = clearErrorStatus,
            )
          }
          return@withContext
        }

        // If we do have a channel, mark shutting down as we clean up
        Timber.d("Shutting down wifi network")
        status.set(
            RunningStatus.Stopping,
            clearError = clearErrorStatus,
        )

        shutdownWifiNetwork(channel)

        completeStop(this, clearErrorStatus) {
          Timber.d("Proxy was stopped")
          status.set(
              RunningStatus.NotRunning,
              clearError = clearErrorStatus,
          )
        }
      }

  @CheckResult
  @SuppressLint("MissingPermission")
  private suspend fun resolveCurrentGroup(channel: Channel): WifiP2pGroup? {
    enforcer.assertOffMainThread()

    return suspendCoroutine { cont ->
      try {
        wifiP2PManager.requestGroupInfo(channel) {
          // We are still on the Main Thread here, so don't unpack anything yet.
          cont.resume(it)
        }
      } catch (e: Throwable) {
        Timber.e(e, "Error getting WiFi Direct Group Info")
        cont.resumeWithException(e)
      }
    }
  }

  @CheckResult
  private suspend fun resolveConnectionInfo(channel: Channel): WifiP2pInfo? {
    enforcer.assertOffMainThread()

    return suspendCoroutine { cont ->
      try {
        wifiP2PManager.requestConnectionInfo(channel) {
          // We are still on the Main Thread here, so don't unpack anything yet.
          cont.resume(it)
        }
      } catch (e: Throwable) {
        Timber.e(e, "Error getting WiFi Direct Connection Info")
        cont.resumeWithException(e)
      }
    }
  }

  @CheckResult
  private fun handleGroupDebugEnvironment(): WiDiNetworkStatus.GroupInfo? {
    enforcer.assertOffMainThread()

    val debugGroup = appEnvironment.group
    if (debugGroup.isEmpty.value) {
      Timber.w("DEBUG forcing Empty group response")
      return WiDiNetworkStatus.GroupInfo.Empty
    }

    if (debugGroup.isError.value) {
      Timber.w("DEBUG forcing Error group response")
      return WiDiNetworkStatus.GroupInfo.Error(
          error = IllegalStateException("DEBUG FORCED ERROR RESPONSE"),
      )
    }

    if (debugGroup.isConnected.value) {
      Timber.w("DEBUG forcing Connected group response")
      return WiDiNetworkStatus.GroupInfo.Connected(
          ssid = "DEBUG SSID",
          password = "DEBUG PASSWORD",
      )
    }

    return null
  }

  @CheckResult
  private suspend fun getGroupInfo(): WiDiNetworkStatus.GroupInfo {
    enforcer.assertOffMainThread()

    if (!permissionGuard.canCreateWiDiNetwork()) {
      Timber.w("Missing permissions, cannot get Group Info")
      return WiDiNetworkStatus.GroupInfo.Empty
    }

    val channel = getChannel()
    if (channel == null) {
      Timber.w("Cannot get group info without Wifi channel")
      return WiDiNetworkStatus.GroupInfo.Empty
    }

    val result: WiDiNetworkStatus.GroupInfo
    val group = resolveCurrentGroup(channel)
    if (group == null) {
      Timber.w("WiFi Direct did not return Group Info")
      result =
          WiDiNetworkStatus.GroupInfo.Error(
              error = IllegalStateException("WiFi Direct did not return Group Info"),
          )
    } else {
      result =
          WiDiNetworkStatus.GroupInfo.Connected(
              ssid = group.networkName,
              password = group.passphrase,
          )
      Timber.d("WiFi Direct Group Info: $result")
    }

    val forcedDebugResult = handleGroupDebugEnvironment()
    if (forcedDebugResult != null) {
      Timber.w("Returning DEBUG result which overrides real: $result")
      return forcedDebugResult
    }

    return result
  }

  @CheckResult
  private fun handleConnectionDebugEnvironment(): WiDiNetworkStatus.ConnectionInfo? {
    enforcer.assertOffMainThread()

    val debugConnection = appEnvironment.connection
    if (debugConnection.isEmpty.value) {
      Timber.w("DEBUG forcing Empty connection response")
      return WiDiNetworkStatus.ConnectionInfo.Empty
    }

    if (debugConnection.isError.value) {
      Timber.w("DEBUG forcing Error connection response")
      return WiDiNetworkStatus.ConnectionInfo.Error(
          error = IllegalStateException("DEBUG FORCED ERROR RESPONSE"),
      )
    }

    if (debugConnection.isConnected.value) {
      Timber.w("DEBUG forcing Connected connection response")
      return WiDiNetworkStatus.ConnectionInfo.Connected(
          ip = "DEBUG IP", hostName = "DEBUG HOSTNAME")
    }

    return null
  }

  @CheckResult
  private suspend fun getConnectionInfo(): WiDiNetworkStatus.ConnectionInfo {
    enforcer.assertOffMainThread()

    if (!permissionGuard.canCreateWiDiNetwork()) {
      Timber.w("Missing permissions, cannot get Connection Info")
      return WiDiNetworkStatus.ConnectionInfo.Empty
    }

    val channel = getChannel()
    if (channel == null) {
      Timber.w("Cannot get connection info without Wifi channel")
      return WiDiNetworkStatus.ConnectionInfo.Empty
    }

    val result: WiDiNetworkStatus.ConnectionInfo
    val info = resolveConnectionInfo(channel)
    val host = info?.groupOwnerAddress
    if (host == null) {
      Timber.w("WiFi Direct did not return Connection Info")
      result =
          WiDiNetworkStatus.ConnectionInfo.Error(
              error = IllegalStateException("WiFi Direct did not return Connection Info"),
          )
    } else {
      result =
          WiDiNetworkStatus.ConnectionInfo.Connected(
              ip = host.hostAddress.orEmpty(),
              hostName = host.hostName.orEmpty(),
          )
      Timber.d("WiFi Direct Connection Info: $result")
    }

    val forcedDebugResult = handleConnectionDebugEnvironment()
    if (forcedDebugResult != null) {
      Timber.w("Returning DEBUG result which overrides real: $result")
      return forcedDebugResult
    }
    return result
  }

  private suspend fun updateNetworkInfoChannels() {
    enforcer.assertOffMainThread()

    groupInfoChannel.value = getGroupInfo()
    connectionInfoChannel.value = getConnectionInfo()
  }

  private suspend fun shutdown() =
      withContext(context = NonCancellable) {
        enforcer.assertOffMainThread()

        Timber.d("Stopping Wi-Fi Direct Network...")
        try {
          stopNetwork(clearErrorStatus = false)
        } catch (e: Throwable) {
          e.ifNotCancellation {
            Timber.e(e, "Error stopping Network")
            status.set(
                RunningStatus.Error(e.message ?: "An error occurred while stopping the Network"))
          }
        } finally {
          Timber.d("Wi-Fi Direct network is shutdown")
        }
      }

  final override suspend fun updateNetworkInfo() =
      // Use Dispatcher.Default here instead since this can run outside of cycle
      withContext(context = Dispatchers.Default) {
        enforcer.assertOffMainThread()

        updateNetworkInfoChannels()
      }

  final override suspend fun start() =
      withContext(context = Dispatchers.Default) {
        enforcer.assertOffMainThread()

        Timber.d("Starting Wi-Fi Direct Network...")
        try {
          stopNetwork(clearErrorStatus = true)

          coroutineScope { startNetwork() }
        } catch (e: Throwable) {
          e.ifNotCancellation {
            Timber.e(e, "Error starting Network")
            status.set(
                RunningStatus.Error(e.message ?: "An error occurred while starting the Network"))
          }
        } finally {
          shutdown()
        }
      }

  final override suspend fun stop(clearErrorStatus: Boolean) =
      withContext(context = Dispatchers.Default) {
        enforcer.assertOffMainThread()
        status.set(RunningStatus.NotRunning, clearErrorStatus)
      }

  final override fun onConnectionInfoChanged(): Flow<WiDiNetworkStatus.ConnectionInfo> {
    return connectionInfoChannel
  }

  final override fun onGroupInfoChanged(): Flow<WiDiNetworkStatus.GroupInfo> {
    return groupInfoChannel
  }

  /** Side effects ran from this function should have their own launch {} */
  protected abstract fun CoroutineScope.onNetworkStarted()

  /** Side effects ran from this function should have their own launch {} */
  protected abstract fun CoroutineScope.onNetworkStopped(clearErrorStatus: Boolean)

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
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
        try {
          s.close()
        } catch (e: Throwable) {
          Timber.e(e, "Failed to close WifiP2P Channel")
        }
      }
    }
  }
}
