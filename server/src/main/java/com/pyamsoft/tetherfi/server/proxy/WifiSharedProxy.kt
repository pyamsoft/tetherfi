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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

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

  private suspend fun beginProxyLoop(type: SharedProxy.Type) {
    enforcer.assertOffMainThread()

    try {
      Timber.d("${type.name} Begin proxy server loop")
      factory.create(type = type).loop()
    } catch (e: Throwable) {
      handleServerLoopError(e, type)
    }
  }

  private fun CoroutineScope.proxyLoop() {
    launch(context = Dispatchers.Default) { beginProxyLoop(SharedProxy.Type.TCP) }
    launch(context = Dispatchers.Default) { beginProxyLoop(SharedProxy.Type.UDP) }
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

  private suspend fun startServer() {
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

        proxyLoop()

        Timber.d("Started Proxy Server")
        status.set(RunningStatus.Running)
      }
    } finally {
      shutdown(clearErrorStatus = false)
    }
  }

  override suspend fun start() =
      withContext(context = Dispatchers.Default) {
        reset()
        try {
          startServer()
        } catch (e: Throwable) {
          e.ifNotCancellation {
            Timber.e(e, "Error when running the proxy, shut it all down")
            reset()

            status.set(RunningStatus.Error(message = e.message ?: "A proxy error occurred"))
            shutdownBus.emit(ServerShutdownEvent)
          }
        }
      }
}
