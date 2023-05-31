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
import com.pyamsoft.pydroid.core.ThreadEnforcer
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.tetherfi.server.event.ServerShutdownEvent
import com.pyamsoft.tetherfi.server.status.RunningStatus
import com.pyamsoft.tetherfi.server.widi.WiDiNetwork
import com.pyamsoft.tetherfi.server.widi.WiDiNetworkStatus
import com.pyamsoft.tetherfi.service.ServiceInternalApi
import com.pyamsoft.tetherfi.service.lock.Locker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ForegroundHandler
@Inject
internal constructor(
    @ServiceInternalApi private val locker: Locker,
    private val enforcer: ThreadEnforcer,
    private val shutdownListener: EventConsumer<ServerShutdownEvent>,
    private val notificationRefreshListener: EventConsumer<NotificationRefreshEvent>,
    private val network: WiDiNetwork,
    private val status: WiDiNetworkStatus,
) {

  private var job: Job? = null

  private suspend fun shutdown() =
      withContext(context = NonCancellable) {
        withContext(context = Dispatchers.Default) { locker.release() }
      }

  fun bind(
      scope: CoroutineScope,
      onShutdownService: () -> Unit,
      onRefreshNotification: () -> Unit,
  ) {
    // Watch everything else as the parent
    val stat = status.requireNotNull()

    job?.cancel()
    job =
        scope.launch {
          enforcer.assertOffMainThread()

          // When shutdown events are received, we kill the service
          shutdownListener.requireNotNull().also { f ->
            f.collect {
              Timber.d("Shutdown event received!")
              onShutdownService()
            }
          }

          // Watch status of network
          stat.onStatusChanged().also { f ->
            launch {
              enforcer.assertOffMainThread()

              f.collect { s ->
                when (s) {
                  is RunningStatus.Error -> {
                    Timber.w("Server Error: ${s.message}")
                    onShutdownService()
                  }
                  else -> Timber.d("Server status changed: $s")
                }
              }
            }
          }

          // Watch status of proxy
          stat.onProxyStatusChanged().also { f ->
            launch {
              enforcer.assertOffMainThread()

              f.collect { s ->
                when (s) {
                  is RunningStatus.Running -> {
                    Timber.d("Proxy Server started!")
                    locker.acquire()
                  }
                  is RunningStatus.Error -> {
                    Timber.w("Proxy Server Error: ${s.message}")
                    onShutdownService()
                  }
                  else -> Timber.d("Proxy status changed: $s")
                }
              }
            }
          }

          // Watch for notification refresh
          notificationRefreshListener.requireNotNull().also { f ->
            launch {
              enforcer.assertOffMainThread()

              f.collect {
                Timber.d("Refresh notification")
                onRefreshNotification()
              }
            }
          }
        }
  }

  suspend fun startProxy() =
      withContext(context = Dispatchers.Default) {
        Timber.d("Start WiDi Network")
        try {
          // Launch a new scope so this function won't proceed to finally block until the scope is
          // completed/cancelled
          //
          // This will suspend until network.start() completes, which is suspended until the proxy
          // server
          // loop dies
          coroutineScope { network.start() }
        } finally {
          shutdown()
        }
      }
}
