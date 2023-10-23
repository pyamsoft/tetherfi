package com.pyamsoft.tetherfi.server.broadcast

import androidx.annotation.CheckResult
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.core.ThreadEnforcer
import com.pyamsoft.pydroid.util.ifNotCancellation
import com.pyamsoft.tetherfi.core.AppDevEnvironment
import com.pyamsoft.tetherfi.core.Timber
import com.pyamsoft.tetherfi.server.BaseServer
import com.pyamsoft.tetherfi.server.broadcast.wifidirect.WiFiDirectError
import com.pyamsoft.tetherfi.server.event.ServerShutdownEvent
import com.pyamsoft.tetherfi.server.prereq.permission.PermissionGuard
import com.pyamsoft.tetherfi.server.status.RunningStatus
import java.time.Clock
import java.time.LocalDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal abstract class BroadcastServer<T : Any>
protected constructor(
    private val shutdownBus: EventBus<ServerShutdownEvent>,
    private val permissionGuard: PermissionGuard,
    private val appEnvironment: AppDevEnvironment,
    private val enforcer: ThreadEnforcer,
    private val clock: Clock,
    status: BroadcastStatus,
) : BaseServer(status), BroadcastNetwork, BroadcastNetworkStatus, BroadcastNetworkUpdater {

  // On some devices, refreshing channel info too frequently leads to errors
  private val groupInfoChannel =
      MutableStateFlow<BroadcastNetworkStatus.GroupInfo>(BroadcastNetworkStatus.GroupInfo.Empty)
  private var lastGroupRefreshTime = LocalDateTime.MIN

  // On some devices, refreshing channel info too frequently leads to errors
  private val connectionInfoChannel =
      MutableStateFlow<BroadcastNetworkStatus.ConnectionInfo>(
          BroadcastNetworkStatus.ConnectionInfo.Empty)
  private var lastConnectionRefreshTime = LocalDateTime.MIN

  private val mutex = Mutex()
  private var proxyJob: Job? = null
  private var heldDataSource: T? = null

  private suspend fun shutdownForStatus(
      newStatus: RunningStatus,
      clearErrorStatus: Boolean,
  ) {
    status.set(newStatus, clearErrorStatus)
    shutdownBus.emit(ServerShutdownEvent)
  }

  @CheckResult
  private suspend fun reUseExistingConnection(dataSource: T): RunningStatus? {
    val fakeError = appEnvironment.isBroadcastFakeError
    if (fakeError.value) {
      Timber.w { "DEBUG forcing Fake Broadcast Error" }
      return WiFiDirectError(
          WiFiDirectError.Reason.Unknown(-1),
          RuntimeException("DEBUG: Force Fake Broadcast Error"),
      )
    }

    val groupInfo = withLockGetGroupInfo(dataSource, force = true)
    val connectionInfo = withLockGetConnectionInfo(dataSource, force = true)
    when (groupInfo) {
      is BroadcastNetworkStatus.GroupInfo.Connected -> {
        when (connectionInfo) {
          is BroadcastNetworkStatus.ConnectionInfo.Connected -> {
            Timber.d { "Re-use existing connection: ${groupInfo.ssid} $connectionInfo" }
            groupInfoChannel.value = groupInfo
            connectionInfoChannel.value = connectionInfo
            return RunningStatus.Running
          }
          is BroadcastNetworkStatus.ConnectionInfo.Empty -> {
            Timber.w { "Connection is EMPTY, cannot re-use" }
            return null
          }
          is BroadcastNetworkStatus.ConnectionInfo.Error -> {
            Timber.w { "Connection is ERROR, cannot re-use" }
            return null
          }
          is BroadcastNetworkStatus.ConnectionInfo.Unchanged -> {
            throw IllegalStateException("Connection.UNCHANGED should not be reached here!")
          }
        }
      }
      is BroadcastNetworkStatus.GroupInfo.Empty -> {
        Timber.w { "Group is EMPTY, cannot re-use" }
        return null
      }
      is BroadcastNetworkStatus.GroupInfo.Error -> {
        Timber.w { "Group is ERROR, cannot re-use" }
        return null
      }
      is BroadcastNetworkStatus.GroupInfo.Unchanged -> {
        throw IllegalStateException("Group.UNCHANGED should not be reached here!")
      }
    }
  }

  private suspend fun withLockStartNetwork() =
      withContext(context = Dispatchers.Default) {
        enforcer.assertOffMainThread()

        // Mark starting
        status.set(
            RunningStatus.Starting,
            clearError = true,
        )

        // Kill the old proxy
        killProxyJob()

        var launchProxy: RunningStatus? = null
        mutex.withLock {
          Timber.d { "START NEW NETWORK" }

          if (!permissionGuard.canCreateWiDiNetwork()) {
            Timber.w { "Missing permissions for making WiDi network" }
            shutdownForStatus(
                RunningStatus.NotRunning,
                clearErrorStatus = false,
            )
            return@withContext
          }

          val dataSource = createDataSource()
          if (dataSource == null) {
            Timber.w { "Failed to create channel, cannot initialize WiDi network" }

            completeStop(this, clearErrorStatus = false) {
              val e = RuntimeException("Failed to create Wi-Fi Direct Channel")
              shutdownForStatus(
                  RunningStatus.HotspotError(e),
                  clearErrorStatus = false,
              )
            }
            return@withContext
          }

          // Re-use the existing group if we can
          // NOTE: If the SSID/password has changed between creating this group in the past and
          // retrieving it now, the UI will be out of sync. Do we care?

          val runningStatus = reUseExistingConnection(dataSource) ?: connectDataSource(dataSource)
          if (runningStatus is RunningStatus.Running) {
            Timber.d { "Network started" }

            // Only store the channel if it successfully "finished" creating.
            Timber.d { "Store data source: $dataSource" }
            heldDataSource = dataSource

            launchProxy = runningStatus
          } else {
            Timber.w { "Group failed creation, stop network" }

            // Remove whatever was created (should be a no-op if everyone follows API correctly)
            shutdownWifiNetwork(dataSource)

            completeStop(this, clearErrorStatus = false) {
              Timber.w { "Stopping network after Group failed to create" }
              shutdownForStatus(
                  runningStatus,
                  clearErrorStatus = false,
              )
            }
          }
        }

        // Run this code outside of the lock because we don't want the proxy loop to block the
        // rest of the lock

        // Do this outside of the lock, since this will run "forever"
        launchProxy?.also {
          val newProxyJob =
              launch(context = Dispatchers.IO) { onNetworkStarted(connectionInfoChannel) }
          Timber.d { "Track new proxy job!" }
          proxyJob = newProxyJob
        }
      }

  private inline fun completeStop(
      scope: CoroutineScope,
      clearErrorStatus: Boolean,
      onStopped: () -> Unit,
  ) {
    enforcer.assertOffMainThread()

    Timber.d { "Reset last info refresh times" }
    lastGroupRefreshTime = LocalDateTime.MIN
    lastConnectionRefreshTime = LocalDateTime.MIN
    connectionInfoChannel.value = BroadcastNetworkStatus.ConnectionInfo.Empty
    groupInfoChannel.value = BroadcastNetworkStatus.GroupInfo.Empty

    scope.launch(context = Dispatchers.Default) { onNetworkStopped(clearErrorStatus) }

    onStopped()
  }

  private suspend fun shutdownWifiNetwork(dataSource: T) {
    enforcer.assertOffMainThread()

    Timber.d { "Close data source" }
    disconnectDataSource(dataSource)

    // Clear out so nobody else can use a dead channel
    heldDataSource = null
  }

  private suspend fun killProxyJob() {
    proxyJob?.also { p ->
      p.cancelAndJoin()
      Timber.d { "Stopped proxy job" }
    }
    proxyJob = null
  }

  private suspend fun withLockStopNetwork(clearErrorStatus: Boolean) =
      withContext(context = Dispatchers.Default) {
        enforcer.assertOffMainThread()

        mutex.withLock {
          Timber.d { "STOP NETWORK" }

          // If we do have a channel, mark shutting down as we clean up
          Timber.d { "Shutting down wifi network" }
          shutdownForStatus(
              RunningStatus.Stopping,
              clearErrorStatus,
          )

          killProxyJob()

          // If we have no channel, we haven't started yet. Make sure we are clean, but this
          // is basically a no-op
          heldDataSource?.also { shutdownWifiNetwork(it) }

          completeStop(this, clearErrorStatus) {
            shutdownForStatus(
                RunningStatus.NotRunning,
                clearErrorStatus,
            )
            Timber.d { "Network was stopped" }
          }
        }
      }

  @CheckResult
  private fun handleGroupDebugEnvironment(): BroadcastNetworkStatus.GroupInfo? {
    enforcer.assertOffMainThread()

    val debugGroup = appEnvironment.group
    if (debugGroup.isEmpty.value) {
      Timber.w { "DEBUG forcing Empty group response" }
      return BroadcastNetworkStatus.GroupInfo.Empty
    }

    if (debugGroup.isError.value) {
      Timber.w { "DEBUG forcing Error group response" }
      return BroadcastNetworkStatus.GroupInfo.Error(
          error = IllegalStateException("DEBUG FORCED ERROR RESPONSE"),
      )
    }

    if (debugGroup.isConnected.value) {
      Timber.w { "DEBUG forcing Connected group response" }
      return BroadcastNetworkStatus.GroupInfo.Connected(
          ssid = "DEBUG SSID",
          password = "DEBUG PASSWORD",
      )
    }

    return null
  }

  @CheckResult
  private suspend fun withLockGetGroupInfo(
      dataSource: T?,
      force: Boolean
  ): BroadcastNetworkStatus.GroupInfo {
    enforcer.assertOffMainThread()

    if (!permissionGuard.canCreateWiDiNetwork()) {
      Timber.w { "Missing permissions, cannot get Group Info" }
      return BroadcastNetworkStatus.GroupInfo.Empty
    }

    if (dataSource == null) {
      Timber.w { "Cannot get group info without Wifi channel" }
      return BroadcastNetworkStatus.GroupInfo.Empty
    }

    val now = LocalDateTime.now(clock)
    if (!force) {
      if (lastGroupRefreshTime.plusSeconds(5L) >= now) {
        return BroadcastNetworkStatus.GroupInfo.Unchanged
      }
    }

    val result = resolveCurrentGroupInfo(dataSource)
    if (result is BroadcastNetworkStatus.GroupInfo.Connected) {
      // Save success time
      lastGroupRefreshTime = now
    }

    val forcedDebugResult = handleGroupDebugEnvironment()
    if (forcedDebugResult != null) {
      Timber.w { "Returning DEBUG result which overrides real: $result" }
      return forcedDebugResult
    }

    return result
  }

  @CheckResult
  private fun handleConnectionDebugEnvironment(): BroadcastNetworkStatus.ConnectionInfo? {
    enforcer.assertOffMainThread()

    val debugConnection = appEnvironment.connection
    if (debugConnection.isEmpty.value) {
      Timber.w { "DEBUG forcing Empty connection response" }
      return BroadcastNetworkStatus.ConnectionInfo.Empty
    }

    if (debugConnection.isError.value) {
      Timber.w { "DEBUG forcing Error connection response" }
      return BroadcastNetworkStatus.ConnectionInfo.Error(
          error = IllegalStateException("DEBUG FORCED ERROR RESPONSE"),
      )
    }

    if (debugConnection.isConnected.value) {
      Timber.w { "DEBUG forcing Connected connection response" }
      return BroadcastNetworkStatus.ConnectionInfo.Connected(hostName = "DEBUG HOSTNAME")
    }

    return null
  }

  @CheckResult
  private suspend fun withLockGetConnectionInfo(
      dataSource: T?,
      force: Boolean
  ): BroadcastNetworkStatus.ConnectionInfo {
    enforcer.assertOffMainThread()

    if (!permissionGuard.canCreateWiDiNetwork()) {
      Timber.w { "Missing permissions, cannot get Connection Info" }
      return BroadcastNetworkStatus.ConnectionInfo.Empty
    }

    if (dataSource == null) {
      Timber.w { "Cannot get connection info without Wifi channel" }
      return BroadcastNetworkStatus.ConnectionInfo.Empty
    }

    val now = LocalDateTime.now(clock)
    if (!force) {
      if (lastConnectionRefreshTime.plusSeconds(10L) > now) {
        return BroadcastNetworkStatus.ConnectionInfo.Unchanged
      }
    }

    val result = resolveCurrentConnectionInfo(dataSource)
    if (result is BroadcastNetworkStatus.ConnectionInfo.Connected) {
      // Save success time
      lastConnectionRefreshTime = now
    }

    val forcedDebugResult = handleConnectionDebugEnvironment()
    if (forcedDebugResult != null) {
      Timber.w { "Returning DEBUG result which overrides real: $result" }
      return forcedDebugResult
    }
    return result
  }

  final override suspend fun updateNetworkInfo() =
      withContext(context = Dispatchers.Default) {
        enforcer.assertOffMainThread()

        mutex.withLock {
          val dataSource = heldDataSource

          val groupInfo = withLockGetGroupInfo(dataSource, force = false)
          if (groupInfo != BroadcastNetworkStatus.GroupInfo.Unchanged) {
            Timber.d { "WiFi Direct Group Info: $groupInfo" }
            groupInfoChannel.value = groupInfo
          } else {
            Timber.w { "Last Group Info request is still fresh, unchanged" }
          }

          val connectionInfo = withLockGetConnectionInfo(dataSource, force = false)
          if (connectionInfo != BroadcastNetworkStatus.ConnectionInfo.Unchanged) {
            Timber.d { "WiFi Direct Connection Info: $connectionInfo" }
            connectionInfoChannel.value = connectionInfo
          } else {
            Timber.w { "Last Connection Info request is still fresh, unchanged" }
          }
        }
      }

  final override suspend fun start() =
      withContext(context = Dispatchers.Default) {
        enforcer.assertOffMainThread()

        if (status.get() is RunningStatus.Error) {
          Timber.w { "Reset network from error state" }
          withLockStopNetwork(clearErrorStatus = true)
        }

        Timber.d { "Starting Wi-Fi Direct Network..." }
        try {
          // Launch a new scope so this function won't proceed to finally block until the scope is
          // completed/cancelled
          coroutineScope {
            onBeforeStartingNetwork(scope = this)

            // This will suspend until onNetworkStart proxy.start() completes,
            // which is suspended until the proxy server loop dies
            withLockStartNetwork()
          }
        } catch (e: Throwable) {
          e.ifNotCancellation {
            Timber.e(e) { "Error starting Network" }
            shutdownForStatus(
                RunningStatus.HotspotError(e),
                clearErrorStatus = false,
            )
          }
        } finally {
          withContext(context = NonCancellable) {
            Timber.d { "Stopping Wi-Fi Direct Network..." }
            withLockStopNetwork(clearErrorStatus = false)
          }
        }
      }

  final override fun onConnectionInfoChanged(): Flow<BroadcastNetworkStatus.ConnectionInfo> {
    return connectionInfoChannel
  }

  final override fun onGroupInfoChanged(): Flow<BroadcastNetworkStatus.GroupInfo> {
    return groupInfoChannel
  }

  /** Side effects ran from this function should have their own launch {} */
  protected abstract fun CoroutineScope.onNetworkStarted(
      connectionStatus: Flow<BroadcastNetworkStatus.ConnectionInfo>,
  )

  /** Side effects ran from this function should have their own launch {} */
  protected abstract fun CoroutineScope.onNetworkStopped(clearErrorStatus: Boolean)

  protected abstract suspend fun onBeforeStartingNetwork(scope: CoroutineScope)

  /** Create data source for implementation */
  @CheckResult protected abstract suspend fun createDataSource(): T?

  /** Connect data source for implementation */
  @CheckResult protected abstract suspend fun connectDataSource(dataSource: T): RunningStatus

  /** Connect data source for implementation */
  protected abstract suspend fun disconnectDataSource(dataSource: T)

  /** Resolve group info for implementation */
  @CheckResult
  protected abstract suspend fun resolveCurrentGroupInfo(
      dataSource: T
  ): BroadcastNetworkStatus.GroupInfo

  /** Resolve connection info for implementation */
  @CheckResult
  protected abstract suspend fun resolveCurrentConnectionInfo(
      dataSource: T
  ): BroadcastNetworkStatus.ConnectionInfo
}
