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

import com.pyamsoft.tetherfi.server.broadcast.BroadcastNetwork
import com.pyamsoft.tetherfi.service.ServiceInternalApi
import com.pyamsoft.tetherfi.service.lock.Locker
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

@Singleton
class ForegroundLauncher
@Inject
internal constructor(
    @param:ServiceInternalApi private val locker: Locker,
    private val network: BroadcastNetwork,
) {

  private suspend fun withLock(block: suspend () -> Unit) {
    try {
      locker.acquire()
      block()
    } finally {
      locker.release()
    }
  }

  suspend fun startProxy() =
      withContext(context = Dispatchers.Default) {
        withLock {
          // Launch a new scope so this function won't proceed to finally block until the scope is
          // completed/cancelled
          //
          // This will suspend until network.start() completes, which is suspended until the proxy
          // server loop dies
          coroutineScope { network.start() }
        }
      }
}
