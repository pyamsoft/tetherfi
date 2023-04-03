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
import com.pyamsoft.tetherfi.core.AppDevEnvironment
import com.pyamsoft.tetherfi.server.BaseServer
import com.pyamsoft.tetherfi.server.ServerDefaults
import com.pyamsoft.tetherfi.server.event.ServerShutdownEvent
import com.pyamsoft.tetherfi.server.permission.PermissionGuard
import com.pyamsoft.tetherfi.server.status.RunningStatus
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber

internal abstract class WifiDirectNetwork
protected constructor(
    private val shutdownBus: EventBus<ServerShutdownEvent>,
    private val appContext: Context,
    private val permissionGuard: PermissionGuard,
    private val dispatcher: CoroutineDispatcher,
    private val config: WiDiConfig,
    private val appEnvironment: AppDevEnvironment,
    private val enforcer: ThreadEnforcer,
    status: WiDiStatus,
) : BaseServer(status), WiDiNetwork, WiDiNetworkStatus {

  private val wifiP2PManager by lazy {
    appContext.getSystemService<WifiP2pManager>().requireNotNull()
  }

  private val scope by lazy { CoroutineScope(context = dispatcher) }

  private val groupInfoChannel =
      MutableStateFlow<WiDiNetworkStatus.GroupInfo>(WiDiNetworkStatus.GroupInfo.Empty)
  private val connectionInfoChannel =
      MutableStateFlow<WiDiNetworkStatus.ConnectionInfo>(WiDiNetworkStatus.ConnectionInfo.Empty)

  private val mutex = Mutex()
  private var wifiChannel: Channel? = null

  @CheckResult
  private fun createChannel(context: CoroutineContext): Channel? {
    Timber.d("Creating WifiP2PManager Channel")

    // This can return null if initialization fails
    return wifiP2PManager.initialize(
        appContext,
        Looper.getMainLooper(),
    ) {
      scope.launch(context = context) {
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

  private suspend fun startNetwork(context: CoroutineContext) =
      withContext(context = context) {
        enforcer.assertOffMainThread()

        if (!permissionGuard.canCreateWiDiNetwork()) {
          Timber.w("Missing permissions for making WiDi network")
          status.set(RunningStatus.NotRunning)
          return@withContext
        }

        Timber.d("Start new network")
        status.set(RunningStatus.Starting)

        val channel = createChannel(context = context)
        if (channel == null) {
          Timber.w("Failed to create channel, cannot initialize WiDi network")

          completeStop { status.set(RunningStatus.Error("Failed to create Wi-Fi Direct Channel")) }
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

          updateNetworkInfoChannels(context = context)
          onNetworkStarted(context = context)
        } else {
          Timber.w("Group failed creation, stop proxy")

          // Remove whatever was created (should be a no-op if everyone follows API correctly)
          shutdownWifiNetwork(channel)

          completeStop { Timber.w("Stopping proxy after Group failed to create") }
        }

        status.set(runningStatus)
      }

  private suspend fun completeStop(onStop: () -> Unit) =
      withContext(context = Dispatchers.Main) {
        val context = dispatcher

        updateNetworkInfoChannels(context = context)
        onNetworkStopped(context = context)
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

  private suspend fun stopNetwork(resetStatus: Boolean) {
    enforcer.assertOffMainThread()

    val channel = getChannel()

    // If we have no channel, we haven't started yet. Make sure we are clean, but this
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
  private suspend fun getGroupInfo(context: CoroutineContext): WiDiNetworkStatus.GroupInfo =
      withContext(context = context) {
        enforcer.assertOffMainThread()

        if (!permissionGuard.canCreateWiDiNetwork()) {
          Timber.w("Missing permissions, cannot get Group Info")
          return@withContext WiDiNetworkStatus.GroupInfo.Empty
        }

        val channel = getChannel()
        if (channel == null) {
          Timber.w("Cannot get group info without Wifi channel")
          return@withContext WiDiNetworkStatus.GroupInfo.Empty
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
          return@withContext forcedDebugResult
        } else {
          return@withContext result
        }
      }

  @CheckResult
  private fun handleConnectionDebugEnvironment(): WiDiNetworkStatus.ConnectionInfo? {
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
  private suspend fun getConnectionInfo(
      context: CoroutineContext
  ): WiDiNetworkStatus.ConnectionInfo =
      withContext(context = context) {
        enforcer.assertOffMainThread()

        if (!permissionGuard.canCreateWiDiNetwork()) {
          Timber.w("Missing permissions, cannot get Connection Info")
          return@withContext WiDiNetworkStatus.ConnectionInfo.Empty
        }

        val channel = getChannel()
        if (channel == null) {
          Timber.w("Cannot get connection info without Wifi channel")
          return@withContext WiDiNetworkStatus.ConnectionInfo.Empty
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
          return@withContext forcedDebugResult
        } else {
          return@withContext result
        }
      }

  private suspend fun updateNetworkInfoChannels(context: CoroutineContext) =
      withContext(context = context) {
        enforcer.assertOffMainThread()

        groupInfoChannel.value = getGroupInfo(context = context)
        connectionInfoChannel.value = getConnectionInfo(context = context)
      }

  final override fun updateNetworkInfo() {
    val context = dispatcher

    scope.launch(context = context) {
      enforcer.assertOffMainThread()

      updateNetworkInfoChannels(context = context)
    }
  }

  final override fun start() {
    val context = dispatcher

    scope.launch(context = context) {
      enforcer.assertOffMainThread()

      Timber.d("Starting Wi-Fi Direct Network...")
      try {
        stopNetwork(resetStatus = true)
        startNetwork(context = context)
      } catch (e: Throwable) {
        Timber.e(e, "Error starting Network")
        status.set(RunningStatus.Error(e.message ?: "An error occurred while starting the Network"))
      }
    }
  }

  final override fun stop() {
    scope.launch(context = dispatcher) {
      enforcer.assertOffMainThread()

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

  override suspend fun onConnectionInfoChanged(
      onChange: (WiDiNetworkStatus.ConnectionInfo) -> Unit
  ) =
      withContext(context = dispatcher) {
        enforcer.assertOffMainThread()
        connectionInfoChannel.collect { onChange(it) }
      }

  override suspend fun onGroupInfoChanged(onChange: (WiDiNetworkStatus.GroupInfo) -> Unit) =
      withContext(context = dispatcher) {
        enforcer.assertOffMainThread()
        groupInfoChannel.collect { onChange(it) }
      }

  protected abstract suspend fun onNetworkStarted(context: CoroutineContext)

  protected abstract suspend fun onNetworkStopped(context: CoroutineContext)

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
