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

import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.core.ThreadEnforcer
import com.pyamsoft.pydroid.util.ifNotCancellation
import com.pyamsoft.tetherfi.server.BaseServer
import com.pyamsoft.tetherfi.server.ServerInternalApi
import com.pyamsoft.tetherfi.server.clients.ClientEraser
import com.pyamsoft.tetherfi.server.event.ServerShutdownEvent
import com.pyamsoft.tetherfi.server.proxy.manager.ProxyManager
import com.pyamsoft.tetherfi.server.status.RunningStatus
import com.pyamsoft.tetherfi.server.widi.WiDiNetworkStatus
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber

@Singleton
internal class WifiSharedProxy
@Inject
internal constructor(
    @ServerInternalApi private val factory: ProxyManager.Factory,
    private val enforcer: ThreadEnforcer,
    private val eraser: ClientEraser,
    private val shutdownBus: EventBus<ServerShutdownEvent>,
    status: ProxyStatus,
) : BaseServer(status), SharedProxy {

  private suspend fun handleServerLoopError(e: Throwable, type: SharedProxy.Type) {
    e.ifNotCancellation {
      Timber.e(e, "Error running server loop: ${type.name}")

      reset()
      status.set(RunningStatus.Error(e.message ?: "An unexpected error occurred."))
      shutdownBus.emit(ServerShutdownEvent)
    }
  }

  private suspend fun beginProxyLoop(
      type: SharedProxy.Type,
      info: WiDiNetworkStatus.ConnectionInfo.Connected,
  ) {
    enforcer.assertOffMainThread()

    try {
      Timber.d("${type.name} Begin proxy server loop: $info")
      factory
          .create(
              type = type,
              info = info,
          )
          .loop()
    } catch (e: Throwable) {
      handleServerLoopError(e, type)
    }
  }

  private fun CoroutineScope.proxyLoop(info: WiDiNetworkStatus.ConnectionInfo.Connected) {
    launch(context = Dispatchers.Default) {
      beginProxyLoop(
          type = SharedProxy.Type.TCP,
          info = info,
      )
    }

    // TODO: UDP support
    //   launch(context = Dispatchers.Default) {
    //     beginProxyLoop(
    //       type = SharedProxy.Type.UDP,
    //       info = info,
    //     )
    //   }
  }

  private fun reset() {
    enforcer.assertOffMainThread()

    eraser.clear()
  }

  private suspend fun shutdown(clearErrorStatus: Boolean) =
      withContext(context = NonCancellable) {
        enforcer.assertOffMainThread()

        Timber.d("Proxy Server is Complete")
        status.set(
            RunningStatus.Stopping,
            clearError = clearErrorStatus,
        )
        reset()
        status.set(RunningStatus.NotRunning)
      }

  private suspend fun startServer(info: WiDiNetworkStatus.ConnectionInfo.Connected) {
    try {
      // Launch a new scope so this function won't proceed to finally block until the scope is
      // completed/cancelled
      //
      // This will suspend until the proxy server loop dies
      coroutineScope {
        Timber.d("Starting proxy server ...")
        status.set(
            RunningStatus.Starting,
            clearError = true,
        )

        proxyLoop(info)

        Timber.d("Started Proxy Server")
        status.set(RunningStatus.Running)
      }
    } finally {
      Timber.d("Stopped Proxy Server")
    }
  }

  override suspend fun start(connectionStatus: Flow<WiDiNetworkStatus.ConnectionInfo>) =
      withContext(context = Dispatchers.Default) {
        // Watch the connection status
        try {
          // Launch a new scope so this function won't proceed to finally block until the scope is
          // completed/cancelled
          //
          // This will suspend until the proxy server loop dies
          coroutineScope {
            // Scope local
            val mutex = Mutex()
            var job: Job? = null

            // Watch the connection status for valid info
            connectionStatus.distinctUntilChanged().collect { info ->
              when (info) {
                is WiDiNetworkStatus.ConnectionInfo.Connected -> {
                  // Connected is good, we can launch
                  // This will re-launch any time the connection info changes
                  mutex.withLock {
                    job?.cancelAndJoin()

                    reset()

                    // Hold onto the job here so we can cancel it if we need to
                    job = launch(context = Dispatchers.Default) { startServer(info) }
                  }
                }
                is WiDiNetworkStatus.ConnectionInfo.Empty -> {
                  Timber.w("Connection EMPTY, shut down Proxy")

                  // Empty is missing the channel, bad
                  mutex.withLock {
                    job?.cancelAndJoin()
                    shutdown(clearErrorStatus = false)
                  }
                }
                is WiDiNetworkStatus.ConnectionInfo.Error -> {
                  Timber.w("Connection ERROR, shut down Proxy")

                  // Error is bad, shut down the proxy
                  mutex.withLock {
                    job?.cancelAndJoin()
                    shutdown(clearErrorStatus = false)
                  }
                }
                is WiDiNetworkStatus.ConnectionInfo.Unchanged -> {
                  Timber.w("UNCHANGED SHOULD NOT HAPPEN")
                  shutdown(clearErrorStatus = false)
                  throw IllegalStateException(
                      "GroupInfo.Unchanged should never escape the server-module internals.")
                }
              }
            }
          }
        } finally {
          Timber.d("Proxy stopped from OUTSIDE")
          shutdown(clearErrorStatus = false)
        }
      }
}
