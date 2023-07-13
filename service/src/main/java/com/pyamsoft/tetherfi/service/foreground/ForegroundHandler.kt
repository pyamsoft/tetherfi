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
import com.pyamsoft.tetherfi.server.widi.WiDiNetwork
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
) {

  private var job: Job? = null

  private suspend fun shutdown() =
      withContext(context = NonCancellable) {
        withContext(context = Dispatchers.Default) { locker.release() }
      }

  private suspend fun lock() = withContext(context = Dispatchers.Default) { locker.acquire() }

  fun bind(
      scope: CoroutineScope,
      onShutdownService: () -> Unit,
      onRefreshNotification: () -> Unit,
  ) {
    // Watch everything else as the parent
    job?.cancel()
    job =
        scope.launch(context = Dispatchers.Default) {
          enforcer.assertOffMainThread()

          // When shutdown events are received, we kill the service
          shutdownListener.requireNotNull().also { f ->
            launch(context = Dispatchers.Default) {
              f.collect {
                Timber.d("Shutdown event received!")
                onShutdownService()
              }
            }
          }

          // Watch for notification refresh
          notificationRefreshListener.requireNotNull().also { f ->
            launch(context = Dispatchers.Default) {
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
          // Claim the wakelock
          lock()

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
