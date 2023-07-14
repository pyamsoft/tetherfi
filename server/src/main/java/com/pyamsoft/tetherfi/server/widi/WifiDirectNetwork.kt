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
import java.time.Clock
import java.time.LocalDateTime
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
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
    private val config: WiDiConfig,
    private val appEnvironment: AppDevEnvironment,
    private val enforcer: ThreadEnforcer,
    private val clock: Clock,
    status: WiDiStatus,
) : BaseServer(status), WiDiNetwork, WiDiNetworkStatus {

  private val wifiP2PManager by lazy {
    appContext.getSystemService<WifiP2pManager>().requireNotNull()
  }

  // On some devices, refreshing channel info too frequently leads to errors
  private val groupInfoChannel =
      MutableStateFlow<WiDiNetworkStatus.GroupInfo>(WiDiNetworkStatus.GroupInfo.Empty)
  private var lastGroupRefreshTime = LocalDateTime.MIN

  // On some devices, refreshing channel info too frequently leads to errors
  private val connectionInfoChannel =
      MutableStateFlow<WiDiNetworkStatus.ConnectionInfo>(WiDiNetworkStatus.ConnectionInfo.Empty)
  private var lastConnectionRefreshTime = LocalDateTime.MIN

  private val mutex = Mutex()
  private var proxyJob: Job? = null
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
      // Before we used to kill the Network
      //
      // But now we do nothing - if you Swipe Away the app from recents,
      // the p2p manager will die, but when it comes back we want everything to
      // attempt to run again so we leave this around.
      //
      // Any other unexpected death like Airplane mode or Wifi off should be covered by the receiver
      // so we should never unintentionally leak the service
      Timber.d("WifiP2PManager Channel died! Do nothing :D")
    }
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
    return suspendCoroutine { cont ->
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

  private suspend fun shutdownForStatus(newStatus: RunningStatus, clearErrorStatus: Boolean) {
    status.set(newStatus, clearErrorStatus)
    shutdownBus.emit(ServerShutdownEvent)
  }

  @CheckResult
  private suspend fun reUseExistingConnection(channel: Channel): RunningStatus? {
    val groupInfo = withLockGetGroupInfo(channel, force = true)
    val connectionInfo = withLockGetConnectionInfo(channel, force = true)
    when (groupInfo) {
      is WiDiNetworkStatus.GroupInfo.Connected -> {
        when (connectionInfo) {
          is WiDiNetworkStatus.ConnectionInfo.Connected -> {
            Timber.d("Re-use existing connection: ${groupInfo.ssid} $connectionInfo")
            groupInfoChannel.value = groupInfo
            connectionInfoChannel.value = connectionInfo
            return RunningStatus.Running
          }
          is WiDiNetworkStatus.ConnectionInfo.Empty -> {
            Timber.w("Connection is EMPTY, cannot re-use")
            return null
          }
          is WiDiNetworkStatus.ConnectionInfo.Error -> {
            Timber.w("Connection is ERROR, cannot re-use")
            return null
          }
          is WiDiNetworkStatus.ConnectionInfo.Unchanged -> {
            throw IllegalStateException("Connection.UNCHANGED should not be reached here!")
          }
        }
      }
      is WiDiNetworkStatus.GroupInfo.Empty -> {
        Timber.w("Group is EMPTY, cannot re-use")
        return null
      }
      is WiDiNetworkStatus.GroupInfo.Error -> {
        Timber.w("Group is ERROR, cannot re-use")
        return null
      }
      is WiDiNetworkStatus.GroupInfo.Unchanged -> {
        throw IllegalStateException("Group.UNCHANGED should not be reached here!")
      }
    }
  }

  private suspend fun withLockStartNetwork() =
      withContext(context = Dispatchers.Default) {
        enforcer.assertOffMainThread()

        var launchProxy: RunningStatus? = null
        mutex.withLock {
          Timber.d("START NEW NETWORK")

          if (!permissionGuard.canCreateWiDiNetwork()) {
            Timber.w("Missing permissions for making WiDi network")
            shutdownForStatus(RunningStatus.NotRunning, clearErrorStatus = false)
            return@withContext
          }

          status.set(RunningStatus.Starting, clearError = true)
          val channel = createChannel()

          if (channel == null) {
            Timber.w("Failed to create channel, cannot initialize WiDi network")

            completeStop(this, clearErrorStatus = false) {
              shutdownForStatus(
                  RunningStatus.Error("Failed to create Wi-Fi Direct Channel"),
                  clearErrorStatus = false,
              )
            }
            return@withContext
          }

          // Re-use the existing group if we can
          // NOTE: If the SSID/password has changed between creating this group in the past and
          // retrieving it now, the UI will be out of sync. Do we care?

          val runningStatus = reUseExistingConnection(channel) ?: createGroup(channel)
          if (runningStatus is RunningStatus.Running) {
            Timber.d("Network started")

            // Only store the channel if it successfully "finished" creating.
            Timber.d("Store WiFi channel")
            wifiChannel = channel

            launchProxy = runningStatus
          } else {
            Timber.w("Group failed creation, stop proxy")

            // Remove whatever was created (should be a no-op if everyone follows API correctly)
            shutdownWifiNetwork(channel)

            completeStop(this, clearErrorStatus = false) {
              Timber.w("Stopping proxy after Group failed to create")
              shutdownForStatus(
                  runningStatus,
                  clearErrorStatus = false,
              )
            }
          }
        }

        // Run this code outside of the lock because we don't want the proxy loop to block the
        // rest of the lock

        // Kill the old one
        killProxyJob()

        // Do this outside of the lock, since this will run "forever"
        launchProxy?.also { lp ->
          val newProxyJob =
              launch(context = Dispatchers.IO) { onNetworkStarted(connectionInfoChannel) }
          Timber.d("Track new proxy job!")
          proxyJob = newProxyJob

          Timber.d("WiDi network has started: $lp")
          status.set(lp)
        }
      }

  private inline fun completeStop(
      scope: CoroutineScope,
      clearErrorStatus: Boolean,
      onStopped: () -> Unit,
  ) {
    enforcer.assertOffMainThread()

    Timber.d("Reset last info refresh times")
    lastGroupRefreshTime = LocalDateTime.MIN
    lastConnectionRefreshTime = LocalDateTime.MIN
    connectionInfoChannel.value = WiDiNetworkStatus.ConnectionInfo.Empty
    groupInfoChannel.value = WiDiNetworkStatus.GroupInfo.Empty

    scope.launch(context = Dispatchers.Default) { onNetworkStopped(clearErrorStatus) }

    onStopped()
  }

  private suspend fun shutdownWifiNetwork(channel: Channel) {
    enforcer.assertOffMainThread()

    // This may fail if WiFi is off, but that's fine since if WiFi is off,
    // the system has already cleaned us up.
    removeGroup(channel)

    // Close the wifi channel now that we are done with it
    Timber.d("Close WiFiP2PManager channel")
    closeSilent(channel)

    // Clear out so nobody else can use a dead channel
    wifiChannel = null
  }

  private fun killProxyJob() {
    proxyJob?.also { p ->
      Timber.d("Stop proxy job")
      p.cancel()
    }
    proxyJob = null
  }

  private suspend fun withLockStopNetwork(clearErrorStatus: Boolean) =
      withContext(context = Dispatchers.Default) {
        enforcer.assertOffMainThread()

        mutex.withLock {
          Timber.d("STOP NETWORK")
          val channel = wifiChannel

          killProxyJob()

          // If we have no channel, we haven't started yet. Make sure we are clean, but this
          // is basically a no-op
          if (channel == null) {
            completeStop(this, clearErrorStatus) {
              Timber.d("Resetting status back to not running")
              shutdownForStatus(RunningStatus.NotRunning, clearErrorStatus)
            }
            return@withContext
          }

          // If we do have a channel, mark shutting down as we clean up
          Timber.d("Shutting down wifi network")
          shutdownForStatus(RunningStatus.Stopping, clearErrorStatus)

          shutdownWifiNetwork(channel)

          completeStop(this, clearErrorStatus) {
            Timber.d("Proxy was stopped")
            shutdownForStatus(RunningStatus.NotRunning, clearErrorStatus)
          }
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
  private suspend fun withLockGetGroupInfo(
      channel: Channel?,
      force: Boolean
  ): WiDiNetworkStatus.GroupInfo {
    enforcer.assertOffMainThread()

    if (!permissionGuard.canCreateWiDiNetwork()) {
      Timber.w("Missing permissions, cannot get Group Info")
      return WiDiNetworkStatus.GroupInfo.Empty
    }

    if (channel == null) {
      Timber.w("Cannot get group info without Wifi channel")
      return WiDiNetworkStatus.GroupInfo.Empty
    }

    val now = LocalDateTime.now(clock)
    if (!force) {
      if (lastGroupRefreshTime.plusSeconds(5L) >= now) {
        return WiDiNetworkStatus.GroupInfo.Unchanged
      }
    }

    val group = resolveCurrentGroup(channel)
    val result: WiDiNetworkStatus.GroupInfo
    if (group == null) {
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
      // Save success time
      lastGroupRefreshTime = now
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
      return WiDiNetworkStatus.ConnectionInfo.Connected(hostName = "DEBUG HOSTNAME")
    }

    return null
  }

  @CheckResult
  private suspend fun withLockGetConnectionInfo(
      channel: Channel?,
      force: Boolean
  ): WiDiNetworkStatus.ConnectionInfo {
    enforcer.assertOffMainThread()

    if (!permissionGuard.canCreateWiDiNetwork()) {
      Timber.w("Missing permissions, cannot get Connection Info")
      return WiDiNetworkStatus.ConnectionInfo.Empty
    }

    if (channel == null) {
      Timber.w("Cannot get connection info without Wifi channel")
      return WiDiNetworkStatus.ConnectionInfo.Empty
    }

    val now = LocalDateTime.now(clock)
    if (!force) {
      if (lastConnectionRefreshTime.plusSeconds(10L) > now) {
        return WiDiNetworkStatus.ConnectionInfo.Unchanged
      }
    }

    val result: WiDiNetworkStatus.ConnectionInfo
    val info = resolveConnectionInfo(channel)
    val host = info?.groupOwnerAddress
    if (host == null) {
      result =
          WiDiNetworkStatus.ConnectionInfo.Error(
              error = IllegalStateException("WiFi Direct did not return Connection Info"),
          )
    } else {
      result =
          WiDiNetworkStatus.ConnectionInfo.Connected(
              hostName = host.hostName.orEmpty(),
          )

      // Save success time
      lastConnectionRefreshTime = now
    }

    val forcedDebugResult = handleConnectionDebugEnvironment()
    if (forcedDebugResult != null) {
      Timber.w("Returning DEBUG result which overrides real: $result")
      return forcedDebugResult
    }
    return result
  }

  final override suspend fun updateNetworkInfo() =
      withContext(context = Dispatchers.Default) {
        enforcer.assertOffMainThread()

        mutex.withLock {
          val channel = wifiChannel

          val groupInfo = withLockGetGroupInfo(channel, force = false)
          if (groupInfo != WiDiNetworkStatus.GroupInfo.Unchanged) {
            Timber.d("WiFi Direct Group Info: $groupInfo")
            groupInfoChannel.value = groupInfo
          } else {
            Timber.w("Last Group Info request is still fresh, unchanged")
          }

          val connectionInfo = withLockGetConnectionInfo(channel, force = false)
          if (connectionInfo != WiDiNetworkStatus.ConnectionInfo.Unchanged) {
            Timber.d("WiFi Direct Connection Info: $connectionInfo")
            connectionInfoChannel.value = connectionInfo
          } else {
            Timber.w("Last Connection Info request is still fresh, unchanged")
          }
        }
      }

  final override suspend fun start() =
      withContext(context = Dispatchers.Default) {
        enforcer.assertOffMainThread()

        if (status.get() is RunningStatus.Error) {
          Timber.w("Reset proxy from error state")
          withLockStopNetwork(clearErrorStatus = true)
        }

        Timber.d("Starting Wi-Fi Direct Network...")
        try {
          // Launch a new scope so this function won't proceed to finally block until the scope is
          // completed/cancelled
          //
          // This will suspend until onNetworkStart proxy.start() completes,
          // which is suspended until the proxy server loop dies
          coroutineScope { withLockStartNetwork() }
        } catch (e: Throwable) {
          e.ifNotCancellation {
            Timber.e(e, "Error starting Network")
            val msg = e.message ?: "An error occurred while starting the Network"
            shutdownForStatus(RunningStatus.Error(msg), clearErrorStatus = false)
          }
        } finally {
          withContext(context = NonCancellable) {
            Timber.d("Stopping Wi-Fi Direct Network...")
            withLockStopNetwork(clearErrorStatus = false)
          }
        }
      }

  final override fun onConnectionInfoChanged(): Flow<WiDiNetworkStatus.ConnectionInfo> {
    return connectionInfoChannel
  }

  final override fun onGroupInfoChanged(): Flow<WiDiNetworkStatus.GroupInfo> {
    return groupInfoChannel
  }

  /** Side effects ran from this function should have their own launch {} */
  protected abstract fun CoroutineScope.onNetworkStarted(
      connectionStatus: Flow<WiDiNetworkStatus.ConnectionInfo>,
  )

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
