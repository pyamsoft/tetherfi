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

package com.pyamsoft.tetherfi.server.proxy

import androidx.annotation.CheckResult
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.core.ThreadEnforcer
import com.pyamsoft.pydroid.util.ifNotCancellation
import com.pyamsoft.tetherfi.core.AppDevEnvironment
import com.pyamsoft.tetherfi.core.Timber
import com.pyamsoft.tetherfi.server.BaseServer
import com.pyamsoft.tetherfi.server.ServerInternalApi
import com.pyamsoft.tetherfi.server.broadcast.BroadcastNetworkStatus
import com.pyamsoft.tetherfi.server.clients.ClientEraser
import com.pyamsoft.tetherfi.server.clients.StartedClients
import com.pyamsoft.tetherfi.server.event.ServerShutdownEvent
import com.pyamsoft.tetherfi.server.proxy.manager.ProxyManager
import com.pyamsoft.tetherfi.server.status.RunningStatus
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancel
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
              tcp = false,
              udp = false,
          ),
      )

  private fun readyState(type: SharedProxy.Type) {
    overallState.update { s ->
      when (type) {
        SharedProxy.Type.TCP -> s.copy(tcp = true)
        SharedProxy.Type.UDP -> s.copy(udp = true)
      }
    }
  }

  private fun resetState() {
    overallState.update {
      it.copy(
          tcp = false,
          udp = false,
      )
    }
  }

  private suspend fun handleServerLoopError(
      e: Throwable,
      type: SharedProxy.Type,
      serverDispatcher: ServerDispatcher,
  ) {
    e.ifNotCancellation {
      Timber.e(e) { "Error running server loop: ${type.name}" }

      reset(serverDispatcher = serverDispatcher)
      status.set(RunningStatus.ProxyError(e))
      shutdownBus.emit(ServerShutdownEvent)
    }
  }

  private suspend fun beginProxyLoop(
      type: SharedProxy.Type,
      info: BroadcastNetworkStatus.ConnectionInfo.Connected,
      serverDispatcher: ServerDispatcher,
  ) {
    enforcer.assertOffMainThread()

    try {
      Timber.d { "${type.name} Begin proxy server loop: $info" }
      factory
          .create(
              type = type,
              info = info,
              serverDispatcher = serverDispatcher,
          )
          .loop { readyState(type) }
    } catch (e: Throwable) {
      handleServerLoopError(
          e = e,
          type = type,
          serverDispatcher = serverDispatcher,
      )
    }
  }

  private fun CoroutineScope.proxyLoop(
      info: BroadcastNetworkStatus.ConnectionInfo.Connected,
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
          type = SharedProxy.Type.TCP,
          info = info,
          serverDispatcher = serverDispatcher,
      )
    }

    // TODO: UDP support
    if (FLAG_ENABLE_UDP) {
      launch(context = Dispatchers.Default) {
        beginProxyLoop(
            type = SharedProxy.Type.UDP,
            info = info,
            serverDispatcher = serverDispatcher,
        )
      }
    }
  }

  private fun reset(serverDispatcher: ServerDispatcher?) {
    enforcer.assertOffMainThread()

    serverDispatcher?.also { stopDispatcher(it) }
    clientEraser.clear()
    resetState()
  }

  private fun CoroutineDispatcher.shutdown() {
    val self = this
    if (self is ExecutorCoroutineDispatcher) {
      Timber.d { "Close Executor Dispatcher" }
      self.close()
    } else {
      Timber.d { "Cancel plain Dispatcher" }
      self.cancel()
    }
  }

  private fun stopDispatcher(serverDispatcher: ServerDispatcher) {
    Timber.d { "Close server dispatcher if possible" }
    if (serverDispatcher.isPrimaryBound) {
      Timber.w { "Can't close an unbounded Primary Dispatcher" }
    } else {
      Timber.d { "Shutdown Primary Dispatcher" }
      serverDispatcher.primary.shutdown()
    }
    Timber.d { "Shutdown SideEffect Dispatcher" }

    serverDispatcher.sideEffect.shutdown()
  }

  private suspend fun shutdown(serverDispatcher: ServerDispatcher?) =
      withContext(context = NonCancellable) {
        enforcer.assertOffMainThread()

        // Update status if we were running
        if (status.get() is RunningStatus.Running) {
          status.set(RunningStatus.Stopping)
        }

        reset(serverDispatcher = serverDispatcher)
        status.set(RunningStatus.NotRunning)
      }

  private fun CoroutineScope.watchServerReadyStatus() {
    // When all proxy bits declare they are ready, the proxy status is "ready"
    overallState
        .map { it.isReady() }
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
      withContext(context = Dispatchers.Default) {
        // Scope local
        val mutex = Mutex()
        var job: Job? = null
        var dispatcher: ServerDispatcher? = null

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

                    reset(serverDispatcher = dispatcher)
                    dispatcher = null

                    // Hold onto the job here so we can cancel it if we need to
                    val serverDispatcher = serverDispatcherFactory.resolve()
                    job =
                        launch(context = Dispatchers.Default) {
                          startServer(
                              info = info,
                              serverDispatcher = serverDispatcher,
                          )
                        }
                    dispatcher = serverDispatcher
                  }
                }
                is BroadcastNetworkStatus.ConnectionInfo.Empty -> {
                  Timber.w { "Connection EMPTY, shut down Proxy" }

                  // Empty is missing the channel, bad
                  mutex.withLock {
                    job?.stopProxyLoop()
                    job = null
                  }
                  shutdown(serverDispatcher = dispatcher)
                  dispatcher = null
                }
                is BroadcastNetworkStatus.ConnectionInfo.Error -> {
                  Timber.w { "Connection ERROR, shut down Proxy" }

                  // Error is bad, shut down the proxy
                  mutex.withLock {
                    job?.stopProxyLoop()
                    job = null
                  }
                  shutdown(serverDispatcher = dispatcher)
                  dispatcher = null
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
            mutex.withLock {
              job?.stopProxyLoop()
              job = null
            }

            shutdown(serverDispatcher = dispatcher)
            dispatcher = null

            Timber.d { "Proxy Server is Done!" }
          }
        }
      }

  private data class ProxyState(
      val tcp: Boolean,
      val udp: Boolean,
  ) {

    @CheckResult
    fun isReady(): Boolean {
      if (!tcp) {
        return false
      }

      return if (FLAG_ENABLE_UDP) udp else true
    }
  }

  companion object {
    private const val FLAG_ENABLE_UDP = false
  }
}
