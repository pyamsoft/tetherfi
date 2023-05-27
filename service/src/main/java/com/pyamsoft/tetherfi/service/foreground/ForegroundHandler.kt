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

package com.pyamsoft.tetherfi.service.foreground

import com.pyamsoft.pydroid.bus.EventConsumer
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.tetherfi.server.event.ServerShutdownEvent
import com.pyamsoft.tetherfi.server.status.RunningStatus
import com.pyamsoft.tetherfi.server.widi.WiDiNetwork
import com.pyamsoft.tetherfi.server.widi.WiDiNetworkStatus
import com.pyamsoft.tetherfi.service.ServiceInternalApi
import com.pyamsoft.tetherfi.service.lock.Locker
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import timber.log.Timber

@Singleton
class ForegroundHandler
@Inject
internal constructor(
    @ServiceInternalApi private val locker: Locker,
    private val shutdownListener: EventConsumer<ServerShutdownEvent>,
    private val notificationRefreshListener: EventConsumer<NotificationRefreshEvent>,
    private val network: WiDiNetwork,
    private val status: WiDiNetworkStatus,
) {

  private val scope by lazy(LazyThreadSafetyMode.NONE) { MainScope() }

  /**
   * Don't cancel this job on destroy. It must listen for the final shutdown event fired from the
   * server
   */
  private var shutdownJob: Job? = null

  private var parentJob: Job? = null

  fun bind(
      onShutdownService: () -> Unit,
      onRefreshNotification: () -> Unit,
  ) {
    // When shutdown events are received, we kill the service
    shutdownJob?.cancel()
    shutdownListener.requireNotNull().also { f ->
      shutdownJob =
          scope.launch(context = Dispatchers.IO) {
            f.collect {
              Timber.d("Shutdown event received!")
              onShutdownService()
            }
          }
    }

    // Watch everything else as the parent
    val stat = status.requireNotNull()
    val s = stat.onStatusChanged()
    val p = stat.onProxyStatusChanged()
    val n = notificationRefreshListener.requireNotNull()

    parentJob?.cancel()
    parentJob =
        scope.launch(context = Dispatchers.IO) {

          // Watch status of network
          launch(context = Dispatchers.IO) {
            s.collect { s ->
              when (s) {
                is RunningStatus.Error -> {
                  Timber.w("Server Server Error: ${s.message}")
                  locker.release()
                }
                else -> Timber.d("Server status changed: $s")
              }
            }
          }

          // Watch status of proxy
          launch(context = Dispatchers.IO) {
            p.collect { s ->
              when (s) {
                is RunningStatus.Running -> {
                  Timber.d("Proxy Server started!")
                  locker.acquire()
                }
                is RunningStatus.Error -> {
                  Timber.w("Proxy Server Error: ${s.message}")
                  locker.release()
                }
                else -> Timber.d("Proxy status changed: $s")
              }
            }
          }

          // Watch for notification refresh
          launch(context = Dispatchers.IO) {
            n.collect {
              Timber.d("Refresh notification")
              onRefreshNotification()
            }
          }
        }
  }

  fun startProxy() {
    Timber.d("Start WiDi Network")
    network.start()
  }

  fun stopProxy() {
    Timber.d("Stop WiDi network")
    network.stop()

    // Launch a parent scope for all jobs
    scope.launch(context = Dispatchers.IO) {
      Timber.d("Destroy CPU wakelock")
      locker.release()
    }

    parentJob?.cancel()
    parentJob = null
  }

  fun destroy() {
    stopProxy()

    shutdownJob?.cancel()
    shutdownJob = null
  }
}
