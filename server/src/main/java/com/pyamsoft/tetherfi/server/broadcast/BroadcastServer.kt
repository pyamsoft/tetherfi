package com.pyamsoft.tetherfi.server.broadcast

import androidx.annotation.CheckResult
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.core.ThreadEnforcer
import com.pyamsoft.pydroid.util.ifNotCancellation
import com.pyamsoft.tetherfi.core.AppDevEnvironment
import com.pyamsoft.tetherfi.core.Timber
import com.pyamsoft.tetherfi.server.BaseServer
import com.pyamsoft.tetherfi.server.event.ServerShutdownEvent
import com.pyamsoft.tetherfi.server.prereq.permission.PermissionGuard
import com.pyamsoft.tetherfi.server.status.RunningStatus
import java.time.Clock
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
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

  data class UpdateResult(
      val group: Boolean,
      val connection: Boolean,
  )

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
  private var heldSource: T? = null

  private suspend fun withLockInitializeNetwork(source: T) =
      withContext(context = Dispatchers.Default) {
        enforcer.assertOffMainThread()

        // Make sure the network is up-to-date before starting the rest of the proxy
        // Need to delay slightly or else connection info does not resolve correctly
        delay(INITIALIZATION_DELAY)

        // Using the lock we already have, force check for updated info
        withLockUpdateNetworkInfo(
            source = source,
            force = true,
            onlyAcceptWhenAllConnected = false,
        )
      }

  private suspend fun startNetwork() =
      withContext(context = Dispatchers.Default) {
        enforcer.assertOffMainThread()

        // Mark starting
        status.set(
            RunningStatus.Starting,
            clearError = true,
        )

        // Kill the old proxy
        killProxyJob()

        Timber.d { "START NEW NETWORK" }

        if (!permissionGuard.canCreateNetwork()) {
          Timber.w { "Missing permissions for making network" }
          val e = RuntimeException("Missing required Permissions")
          shutdownForStatus(
              RunningStatus.HotspotError(e),
              clearErrorStatus = false,
          )
          return@withContext
        }

        var launchProxy = false
        mutex.withLock {
          try {
            Timber.d { "Starting broadcast network" }
            val source = withLockStartBroadcast()

            // Only store the channel if it successfully "finished" creating.
            Timber.d { "Network started, store data source: $source" }
            heldSource = source

            launchProxy = true

            withLockInitializeNetwork(source = source)
          } catch (e: Throwable) {
            Timber.w { "Error during broadcast startup, stop network" }

            completeStop(this, clearErrorStatus = false) {
              Timber.w { "Stopping network after startup failed" }
              shutdownForStatus(
                  RunningStatus.HotspotError(e),
                  clearErrorStatus = false,
              )
            }
          }
        }

        // Run this code outside of the lock because we don't want the proxy loop to block the
        // rest of the lock

        // Do this outside of the lock, since this will run "forever"
        if (launchProxy) {
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

  private suspend fun shutdownWifiNetwork(source: T) {
    enforcer.assertOffMainThread()

    Timber.d { "Stop broadcast server source" }
    withLockStopBroadcast(source)

    // Clear out so nobody else can use a dead channel
    heldSource = null
  }

  private suspend fun killProxyJob() {
    proxyJob?.also { p ->
      p.cancelAndJoin()
      Timber.d { "Stopped proxy job" }
    }
    proxyJob = null
  }

  private suspend fun stopNetwork(clearErrorStatus: Boolean) =
      withContext(context = Dispatchers.Default) {
        enforcer.assertOffMainThread()

        mutex.withLock {
          Timber.d { "STOP NETWORK" }

          // If we do have a channel, mark shutting down as we clean up
          Timber.d { "Shutting down network" }
          status.set(RunningStatus.Stopping)

          killProxyJob()

          // If we have no channel, we haven't started yet. Make sure we are clean, but this
          // is basically a no-op
          heldSource?.also { shutdownWifiNetwork(it) }

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
      source: T?,
      force: Boolean
  ): BroadcastNetworkStatus.GroupInfo {
    enforcer.assertOffMainThread()

    if (source == null) {
      Timber.w { "Cannot get group info without Wifi source" }
      return BroadcastNetworkStatus.GroupInfo.Empty
    }

    if (!permissionGuard.canCreateNetwork()) {
      Timber.w { "Missing permissions, cannot get Group Info" }
      return BroadcastNetworkStatus.GroupInfo.Empty
    }

    val now = LocalDateTime.now(clock)
    if (!force) {
      if (lastGroupRefreshTime.plusSeconds(REFRESH_DEBOUNCE) >= now) {
        return BroadcastNetworkStatus.GroupInfo.Unchanged
      }
    }

    val result = resolveCurrentGroupInfo(source)
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
      source: T?,
      force: Boolean
  ): BroadcastNetworkStatus.ConnectionInfo {
    enforcer.assertOffMainThread()

    if (source == null) {
      Timber.w { "Cannot get connection info without Wifi source" }
      return BroadcastNetworkStatus.ConnectionInfo.Empty
    }

    if (!permissionGuard.canCreateNetwork()) {
      Timber.w { "Missing permissions, cannot get Connection Info" }
      return BroadcastNetworkStatus.ConnectionInfo.Empty
    }

    val now = LocalDateTime.now(clock)
    if (!force) {
      if (lastConnectionRefreshTime.plusSeconds(REFRESH_DEBOUNCE) > now) {
        return BroadcastNetworkStatus.ConnectionInfo.Unchanged
      }
    }

    val result = resolveCurrentConnectionInfo(source)
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

  private suspend fun shutdownForStatus(
      newStatus: RunningStatus,
      clearErrorStatus: Boolean,
  ) {
    status.set(newStatus, clearErrorStatus)
    shutdownBus.emit(ServerShutdownEvent)
  }

  /**
   * Attempt to update network info for Group and Connection
   *
   * At the point this function is run, we already claim the lock
   */
  @CheckResult
  protected suspend fun withLockUpdateNetworkInfo(
      source: T?,
      // Force rechecking instead of allowing UNCHANGED to be returned because of minimum time
      // between requests
      force: Boolean,
      // For re-using connections when possible, ALL network data must be available and valid
      onlyAcceptWhenAllConnected: Boolean,
  ): UpdateResult =
      withContext(context = Dispatchers.Default) {
        enforcer.assertOffMainThread()

        val groupInfo = withLockGetGroupInfo(source, force = force)
        val connectionInfo = withLockGetConnectionInfo(source, force = force)

        val acceptGroup: Boolean
        val acceptConnection: Boolean
        if (onlyAcceptWhenAllConnected) {
          acceptGroup = groupInfo is BroadcastNetworkStatus.GroupInfo.Connected
          acceptConnection = connectionInfo is BroadcastNetworkStatus.ConnectionInfo.Connected
        } else {
          acceptGroup = groupInfo != BroadcastNetworkStatus.GroupInfo.Unchanged
          acceptConnection = connectionInfo != BroadcastNetworkStatus.ConnectionInfo.Unchanged
        }

        if (onlyAcceptWhenAllConnected) {
          if (acceptGroup && acceptConnection) {
            Timber.d { "Network info update accepted: GRP=$groupInfo CON=$connectionInfo" }
            groupInfoChannel.value = groupInfo
            connectionInfoChannel.value = connectionInfo
          }
        } else {
          if (acceptGroup) {
            Timber.d { "Network update Group=$groupInfo" }
            groupInfoChannel.value = groupInfo
          } else {
            Timber.w { "Network update group not accepted: $groupInfo" }
          }

          if (acceptConnection) {
            Timber.d { "Network update Connection=$connectionInfo" }
            connectionInfoChannel.value = connectionInfo
          } else {
            Timber.w { "Network update connection not accepted: $groupInfo" }
          }
        }

        return@withContext UpdateResult(
            group = acceptGroup,
            connection = acceptConnection,
        )
      }

  final override suspend fun updateNetworkInfo() =
      withContext(context = Dispatchers.Default) {
        mutex.withLock {
          val source = heldSource

          Timber.d { "Attempt update network info with source $source" }

          withLockUpdateNetworkInfo(
              source = source,
              force = false,
              onlyAcceptWhenAllConnected = true,
          )
        }

        // No return
        return@withContext
      }

  final override suspend fun start() =
      withContext(context = Dispatchers.Default) {
        enforcer.assertOffMainThread()

        if (status.get() is RunningStatus.Error) {
          Timber.w { "Reset network from error state" }
          stopNetwork(clearErrorStatus = true)
        }

        Timber.d { "Starting Wifi Network..." }
        try {
          // Launch a new scope so this function won't proceed to finally block until the scope is
          // completed/cancelled
          coroutineScope {
            // This will suspend until onNetworkStart proxy.start() completes,
            // which is suspended until the proxy server loop dies
            startNetwork()
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
            Timber.d { "Stopping Wifi Network..." }
            stopNetwork(clearErrorStatus = false)
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

  /**
   * Connect data source for implementation
   *
   * At the point this function is run, we already claim the lock
   */
  @CheckResult protected abstract suspend fun withLockStartBroadcast(): T

  /**
   * Connect data source for implementation
   *
   * At the point this function is run, we already claim the lock
   */
  protected abstract suspend fun withLockStopBroadcast(source: T)

  /** Resolve group info for implementation */
  @CheckResult
  protected abstract suspend fun resolveCurrentGroupInfo(
      source: T
  ): BroadcastNetworkStatus.GroupInfo

  /** Resolve connection info for implementation */
  @CheckResult
  protected abstract suspend fun resolveCurrentConnectionInfo(
      source: T
  ): BroadcastNetworkStatus.ConnectionInfo

  companion object {
    
    /** Initialization needs to be delayed slightly or Connection info fails to resolve */
    private val INITIALIZATION_DELAY = 750.milliseconds

    /** Don't allow refresh unless forced within this window */
    private const val REFRESH_DEBOUNCE = 3L
  }
}
