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

package com.pyamsoft.tetherfi.server

import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.tetherfi.core.AppCoroutineScope
import com.pyamsoft.tetherfi.core.Timber
import com.pyamsoft.tetherfi.server.broadcast.BroadcastStatus
import com.pyamsoft.tetherfi.server.event.ServerStopRequestEvent
import com.pyamsoft.tetherfi.server.proxy.ProxyStatus
import com.pyamsoft.tetherfi.server.status.RunningStatus
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers

@Singleton
class ServerStopBroadcaster
@Inject
internal constructor(
    private val appScope: AppCoroutineScope,
    private val proxy: ProxyStatus,
    private val wifiDirect: BroadcastStatus,
    private val stopRequestBroadcaster: EventBus<ServerStopRequestEvent>
) {

  private suspend fun beforeStop() {
    Timber.d { "Mark hotspot stopping" }
    proxy.set(RunningStatus.Stopping)
    wifiDirect.set(RunningStatus.Stopping)

    Timber.d { "Broadcast stop-request" }
    stopRequestBroadcaster.emit(ServerStopRequestEvent)
  }

  fun prepareStop(onStopped: () -> Unit) {
    appScope.launch(context = Dispatchers.Default) {
      try {
        beforeStop()
      } finally {
        onStopped()
      }
    }
  }
}
