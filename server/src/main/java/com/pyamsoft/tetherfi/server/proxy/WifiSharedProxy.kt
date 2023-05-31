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
import com.pyamsoft.pydroid.core.ThreadEnforcer
import com.pyamsoft.pydroid.util.ifNotCancellation
import com.pyamsoft.tetherfi.server.BaseServer
import com.pyamsoft.tetherfi.server.ServerInternalApi
import com.pyamsoft.tetherfi.server.ServerPreferences
import com.pyamsoft.tetherfi.server.clients.ClientEraser
import com.pyamsoft.tetherfi.server.proxy.manager.ProxyManager
import com.pyamsoft.tetherfi.server.status.RunningStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
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
    private val preferences: ServerPreferences,
    private val eraser: ClientEraser,
    status: ProxyStatus,
) : BaseServer(status), SharedProxy {

  /** Get the port for the proxy */
  @CheckResult
  private suspend fun getPort(): Int {
    enforcer.assertOffMainThread()

    return preferences.listenForPortChanges().first()
  }

  private fun CoroutineScope.proxyLoop(
      type: SharedProxy.Type,
      port: Int,
  ) {
    launch(context = Dispatchers.Default) {
      val manager = factory.create(type = type)
      Timber.d("${type.name} Begin proxy server loop $port")
      manager.loop(port)
    }
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

  override suspend fun start() =
      withContext(context = Dispatchers.Default) {
        enforcer.assertOffMainThread()

        reset()
        try {
          val port = getPort()
          if (port > 65000 || port <= 1024) {
            Timber.w("Port is invalid: $port")
            reset()
            status.set(RunningStatus.Error(message = "Port is invalid: $port"))
            return@withContext
          }

          try {
            coroutineScope {
              Timber.d("Starting proxy server on port $port ...")
              status.set(RunningStatus.Starting, clearError = true)

              proxyLoop(type = SharedProxy.Type.TCP, port = port)

              Timber.d("Started Proxy Server on port: $port")
              status.set(RunningStatus.Running)
            }
          } finally {
            shutdown(clearErrorStatus = false)
          }
        } catch (e: Throwable) {
          e.ifNotCancellation {
            Timber.e(e, "Error when running the proxy, shut it all down")
            reset()
            status.set(RunningStatus.Error(message = e.message ?: "A proxy error occurred"))
          }
        }
      }
}
