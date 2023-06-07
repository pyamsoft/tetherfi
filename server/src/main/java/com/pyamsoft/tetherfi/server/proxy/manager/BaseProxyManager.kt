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

package com.pyamsoft.tetherfi.server.proxy.manager

import androidx.annotation.CheckResult
import com.pyamsoft.pydroid.core.ThreadEnforcer
import com.pyamsoft.tetherfi.server.proxy.session.tagSocket
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.ASocket
import io.ktor.network.sockets.SocketBuilder
import io.ktor.network.sockets.aSocket
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

internal abstract class BaseProxyManager<S : ASocket>(
    private val dispatcher: CoroutineDispatcher,
    private val enforcer: ThreadEnforcer,
) : ProxyManager {

  override suspend fun loop() =
      withContext(context = dispatcher) {
        enforcer.assertOffMainThread()

        // Tag sockets for Android O strict mode
        tagSocket()

        val manager = SelectorManager(dispatcher = dispatcher)
        val socket = aSocket(manager)
        val server = openServer(builder = socket)
        try {
          runServer(server)
        } finally {
          withContext(context = NonCancellable) {
            // We use Dispatchers.IO because manager.close() could potentially block
            // which, if we used Dispatchers.Default could starve the thread.
            // By using Dispatchers.IO we ensure this block runs on its own pooled thread
            // instead, so even if this blocks it will not resource starve others.
            withContext(context = Dispatchers.IO) {
              server.dispose()
              manager.close()
            }
          }
        }
      }

  protected abstract suspend fun runServer(server: S)

  @CheckResult protected abstract suspend fun openServer(builder: SocketBuilder): S
}
