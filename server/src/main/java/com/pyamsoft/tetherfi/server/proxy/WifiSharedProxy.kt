/*
 * Copyright 2024 pyamsoft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pyamsoft.tetherfi.server.proxy

import androidx.annotation.CheckResult
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.core.ThreadEnforcer
import com.pyamsoft.pydroid.util.ifNotCancellation
import com.pyamsoft.tetherfi.core.AppDevEnvironment
import com.pyamsoft.tetherfi.core.FeatureFlags
import com.pyamsoft.tetherfi.core.Timber
import com.pyamsoft.tetherfi.server.BaseServer
import com.pyamsoft.tetherfi.server.ServerInternalApi
import com.pyamsoft.tetherfi.server.SocketCreator
import com.pyamsoft.tetherfi.server.broadcast.BroadcastNetworkStatus
import com.pyamsoft.tetherfi.server.clients.ClientEraser
import com.pyamsoft.tetherfi.server.clients.StartedClients
import com.pyamsoft.tetherfi.server.event.ServerShutdownEvent
import com.pyamsoft.tetherfi.server.proxy.manager.ProxyManager
import com.pyamsoft.tetherfi.server.status.RunningStatus
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Singleton
internal class WifiSharedProxy
@Inject
internal constructor(
    @ServerInternalApi private val serverDispatcherFactory: ServerDispatcher.Factory,
    @ServerInternalApi private val factory: ProxyManager.Factory,
    private val featureFlags: FeatureFlags,
    private val enforcer: ThreadEnforcer,
    private val clientEraser: ClientEraser,
    private val startedClients: StartedClients,
    private val shutdownBus: EventBus<ServerShutdownEvent>,
    private val appEnvironment: AppDevEnvironment,
    status: ProxyStatus,
) : BaseServer(status), SharedProxy {

  private val overallState =
      MutableStateFlow(
          ProxyState(
              http = false,
              socks = false,
          ),
      )

  private fun adjustState(type: SharedProxy.Type, ready: Boolean) {
    overallState.update { s ->
      when (type) {
        SharedProxy.Type.HTTP -> s.copy(http = ready)
        SharedProxy.Type.SOCKS -> s.copy(socks = ready)
      }
    }
  }

  private fun readyState(type: SharedProxy.Type) {
    adjustState(type, ready = true)
  }

  private fun unreadyState(type: SharedProxy.Type) {
    adjustState(type, ready = false)
  }

  private fun resetState() {
    overallState.update {
      it.copy(
          http = false,
          socks = false,
      )
    }
  }

  private suspend fun handleServerLoopError(
      e: Throwable,
      type: SharedProxy.Type,
  ) {
    Timber.e(e) { "Error running server loop: ${type.name}" }

    reset()
    status.set(RunningStatus.ProxyError(e))
    shutdownBus.emit(ServerShutdownEvent)
  }

  private suspend fun beginProxyLoop(
      type: SharedProxy.Type,
      info: BroadcastNetworkStatus.ConnectionInfo.Connected,
      socketCreator: SocketCreator,
      serverDispatcher: ServerDispatcher,
  ) {
    enforcer.assertOffMainThread()

    try {
      Timber.d { "${type.name} Begin proxy server loop: $info" }
      factory
          .create(
              type = type,
              info = info,
              socketCreator = socketCreator,
              serverDispatcher = serverDispatcher,
          )
          .loop(
              onOpened = { readyState(type) },
              onClosing = {
                // Closing, we mark as stopping early
                status.set(RunningStatus.Stopping)
                unreadyState(type)
              },
              onError = { e ->
                e.ifNotCancellation {
                  handleServerLoopError(
                      e = e,
                      type = type,
                  )
                }
              },
          )
    } catch (e: Throwable) {
      e.ifNotCancellation {
        handleServerLoopError(
            e = e,
            type = type,
        )
      }
    }
  }

  private fun CoroutineScope.proxyLoop(
      info: BroadcastNetworkStatus.ConnectionInfo.Connected,
      socketCreator: SocketCreator,
      serverDispatcher: ServerDispatcher,
  ) {
    val fakeError = appEnvironment.isProxyFakeError
    if (fakeError.value) {
      Timber.w { "DEBUG forcing Fake Proxy Error" }
      status.set(
          RunningStatus.ProxyError(
              RuntimeException("DEBUG: Force Fake Proxy Error"),
          ),
      )
      return
    }

    launch(context = Dispatchers.Default) {
      beginProxyLoop(
          type = SharedProxy.Type.HTTP,
          info = info,
          socketCreator = socketCreator,
          serverDispatcher = serverDispatcher,
      )
    }

    if (featureFlags.isSocksProxyEnabled) {
      launch(context = Dispatchers.Default) {
        beginProxyLoop(
            type = SharedProxy.Type.SOCKS,
            info = info,
            socketCreator = socketCreator,
            serverDispatcher = serverDispatcher,
        )
      }
    }
  }

  private fun reset() {
    enforcer.assertOffMainThread()

    clientEraser.clear()
    resetState()
  }

  private suspend fun broadcastProxyStop() =
      withContext(context = NonCancellable) {
        enforcer.assertOffMainThread()

        // Update status if we were running
        if (status.get() is RunningStatus.Running) {
          status.set(RunningStatus.Stopping)
        }

        reset()
        status.set(RunningStatus.NotRunning)
      }

  private fun CoroutineScope.watchServerReadyStatus() {
    // When all proxy bits declare they are ready, the proxy status is "ready"
    overallState
        .map { it.isReady(featureFlags) }
        .filter { it }
        .also { f ->
          launch(context = Dispatchers.Default) {
            f.collect { ready ->
              if (ready) {
                Timber.d { "Proxy has fully launched, update status!" }
                status.set(
                    RunningStatus.Running,
                    clearError = true,
                )
              }
            }
          }
        }
  }

  private suspend fun startServer(
      info: BroadcastNetworkStatus.ConnectionInfo.Connected,
      socketCreator: SocketCreator,
      serverDispatcher: ServerDispatcher,
  ) {
    try {
      // Launch a new scope so this function won't proceed to finally block until the scope is
      // completed/cancelled
      //
      // This will suspend until the proxy server loop dies
      coroutineScope {
        // Mark proxy launching
        Timber.d { "Starting proxy server ..." }
        status.set(
            RunningStatus.Starting,
            clearError = true,
        )

        watchServerReadyStatus()

        // Notify the client connection watcher that we have started
        launch(context = Dispatchers.Default) { startedClients.started() }

        // Start the proxy server loop
        launch(context = Dispatchers.Default) {
          proxyLoop(
              info = info,
              socketCreator = socketCreator,
              serverDispatcher = serverDispatcher,
          )
        }
      }
    } finally {
      Timber.d { "Stopped Proxy Server" }
    }
  }

  private suspend fun Job.stopProxyLoop() {
    status.set(RunningStatus.Stopping)
    cancelAndJoin()
  }

  override suspend fun start(connectionStatus: Flow<BroadcastNetworkStatus.ConnectionInfo>) =
      withContext(context = Dispatchers.IO) {
        // Scope local
        val mutex = Mutex()
        var job: Job? = null

        // Create the server dispatcher here that future proxy bits will use
        val serverDispatcher = serverDispatcherFactory.create()

        // Create the socket creator for socket connections
        val socketCreator = SocketCreator.create(serverDispatcher)

        // Watch the connection status
        try {
          // Launch a new scope so this function won't proceed to finally block until the scope is
          // completed/cancelled
          //
          // This will suspend until the proxy server loop dies
          coroutineScope {

            // Watch the connection status for valid info
            connectionStatus.distinctUntilChanged().collect { info ->
              when (info) {
                is BroadcastNetworkStatus.ConnectionInfo.Connected -> {
                  // Connected is good, we can launch
                  // This will re-launch any time the connection info changes

                  mutex.withLock {
                    job?.stopProxyLoop()
                    job = null

                    // Reset old
                    reset()

                    // Hold onto the job here so we can cancel it if we need to
                    job =
                        launch(context = Dispatchers.Default) {
                          startServer(
                              info = info,
                              socketCreator = socketCreator,
                              serverDispatcher = serverDispatcher,
                          )
                        }
                  }
                }
                is BroadcastNetworkStatus.ConnectionInfo.Empty -> {
                  Timber.w { "Connection EMPTY, shut down Proxy" }

                  // Empty is missing the channel, bad
                  mutex.withLock {
                    job?.stopProxyLoop()
                    job = null
                  }
                  broadcastProxyStop()
                }
                is BroadcastNetworkStatus.ConnectionInfo.Error -> {
                  Timber.w { "Connection ERROR, shut down Proxy" }

                  // Error is bad, shut down the proxy
                  mutex.withLock {
                    job?.stopProxyLoop()
                    job = null
                  }
                  broadcastProxyStop()
                }
                is BroadcastNetworkStatus.ConnectionInfo.Unchanged -> {
                  Timber.w { "UNCHANGED SHOULD NOT HAPPEN" }
                  throw AssertionError(
                      "GroupInfo.Unchanged should never escape the server-module internals.",
                  )
                }
              }
            }
          }
        } finally {
          withContext(context = NonCancellable) {
            Timber.d { "Shutting down proxy..." }

            // Kill proxy job
            mutex.withLock {
              job?.stopProxyLoop()
              job = null
            }
            // Stop dispatcher looper
            serverDispatcher.shutdown()

            // Broadcast server shutdown
            broadcastProxyStop()

            Timber.d { "Proxy Server is Done!" }
          }
        }
      }

  private data class ProxyState(val http: Boolean, val socks: Boolean) {

    @CheckResult
    fun isReady(featureFlags: FeatureFlags): Boolean {
      if (!http) {
        return false
      }

      if (featureFlags.isSocksProxyEnabled) {
        if (!socks) {
          return false
        }
      }

      return true
    }
  }
}
