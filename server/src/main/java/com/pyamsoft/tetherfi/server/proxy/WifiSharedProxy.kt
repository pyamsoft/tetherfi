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
import com.pyamsoft.tetherfi.server.BaseServer
import com.pyamsoft.tetherfi.server.ServerInternalApi
import com.pyamsoft.tetherfi.server.ServerPreferences
import com.pyamsoft.tetherfi.server.clients.ClientEraser
import com.pyamsoft.tetherfi.server.dispatcher.ProxyDispatcher
import com.pyamsoft.tetherfi.server.proxy.manager.ProxyManager
import com.pyamsoft.tetherfi.server.status.RunningStatus
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
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
    private val dispatcher: ProxyDispatcher,
    private val enforcer: ThreadEnforcer,
    private val preferences: ServerPreferences,
    private val eraser: ClientEraser,
    status: ProxyStatus,
) : BaseServer(status), SharedProxy {

  private val mutex = Mutex()
  private val jobs = mutableListOf<ProxyJob>()

  /** Get the port for the proxy */
  @CheckResult
  private suspend fun getPort(): Int {
    enforcer.assertOffMainThread()

    return preferences.listenForPortChanges().first()
  }

  @CheckResult
  private fun CoroutineScope.proxyLoop(
      type: SharedProxy.Type,
      port: Int,
  ): ProxyJob {
    enforcer.assertOffMainThread()

    val manager = factory.create(type = type)

    val job =
        launch(context = dispatcher.ensureActiveDispatcher()) {
          enforcer.assertOffMainThread()

          Timber.d("${type.name} Begin proxy server loop $port")
          manager.loop(port)
        }

    return ProxyJob(type = type, job = job)
  }

  private suspend fun shutdown() {
    enforcer.assertOffMainThread()

    clearJobs()
    eraser.clear()
  }

  private suspend fun clearJobs() {
    mutex.withLock {
      enforcer.assertOffMainThread()

      jobs.removeEach { proxyJob ->
        Timber.d("Cancelling proxyJob: $proxyJob")
        proxyJob.job.cancel()
      }
    }
  }

  private inline fun <T> MutableList<T>.removeEach(block: (T) -> Unit) {
    while (this.isNotEmpty()) {
      block(this.removeFirst())
    }
  }

  override suspend fun start() =
      withContext(context = Dispatchers.Default) {
        enforcer.assertOffMainThread()

        shutdown()
        try {
          val port = getPort()
          if (port > 65000 || port <= 1024) {
            Timber.w("Port is invalid: $port")
            status.set(RunningStatus.Error(message = "Port is invalid: $port"))
            return@withContext
          }

          Timber.d("Starting proxy server on port $port ...")
          status.set(RunningStatus.Starting, clearError = true)

          launch(context = dispatcher.ensureActiveDispatcher()) {
            enforcer.assertOffMainThread()

            val tcp = proxyLoop(type = SharedProxy.Type.TCP, port = port)

            mutex.withLock { jobs.add(tcp) }

            Timber.d("Started Proxy Server on port: $port")
            status.set(RunningStatus.Running)
          }
        } catch (e: Throwable) {
          Timber.e(e, "Error when running the proxy, shut it all down")
          shutdown()
          status.set(RunningStatus.Error(message = e.message ?: "A proxy error occurred"))
        }
      }

  override suspend fun stop(clearErrorStatus: Boolean) =
      withContext(context = Dispatchers.Default) {
        enforcer.assertOffMainThread()

        status.set(
            RunningStatus.Stopping,
            clearError = clearErrorStatus,
        )

        shutdown()
        status.set(RunningStatus.NotRunning)
      }

  private data class ProxyJob(
      val type: SharedProxy.Type,
      val job: Job,
  )
}
