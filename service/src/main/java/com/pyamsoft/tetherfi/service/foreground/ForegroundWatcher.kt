/*
 * Copyright 2025 pyamsoft
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

package com.pyamsoft.tetherfi.service.foreground

import com.pyamsoft.pydroid.bus.EventConsumer
import com.pyamsoft.pydroid.core.ThreadEnforcer
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.tetherfi.core.Timber
import com.pyamsoft.tetherfi.server.event.ServerShutdownEvent
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ForegroundWatcher
@Inject
internal constructor(
    private val enforcer: ThreadEnforcer,
    private val shutdownListener: EventConsumer<ServerShutdownEvent>,
    private val notificationRefreshListener: EventConsumer<NotificationRefreshEvent>,
) {

  suspend fun bind(
      onShutdownService: suspend (Throwable?) -> Unit,
      onRefreshNotification: suspend () -> Unit,
  ) =
      withContext(context = Dispatchers.Default) {
        val scope = this

        // Watch everything else as the parent
        scope.launch(context = Dispatchers.Default) {
          enforcer.assertOffMainThread()

          // When shutdown events are received, we kill the service
          shutdownListener.requireNotNull().also { f ->
            launch(context = Dispatchers.Default) {
              f.collect {
                Timber.d { "Shutdown event received!" }
                onShutdownService(it.throwable)
              }
            }
          }

          // Watch for notification refresh
          notificationRefreshListener.requireNotNull().also { f ->
            launch(context = Dispatchers.Default) {
              f.collect {
                Timber.d { "Refresh notification" }
                onRefreshNotification()
              }
            }
          }
        }
      }
}
