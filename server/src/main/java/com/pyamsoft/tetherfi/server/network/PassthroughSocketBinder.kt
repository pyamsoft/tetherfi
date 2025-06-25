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

package com.pyamsoft.tetherfi.server.network

import com.pyamsoft.tetherfi.core.Timber
import io.ktor.network.sockets.Socket
import java.net.DatagramSocket
import javax.inject.Inject
import javax.inject.Singleton

// https://github.com/pyamsoft/tetherfi/issues/154
// https://github.com/pyamsoft/tetherfi/issues/331
@Singleton
internal class PassthroughSocketBinder @Inject internal constructor() : SocketBinder {

  override suspend fun withMobileDataNetworkActive(
      block: suspend (SocketBinder.NetworkBinder) -> Unit
  ) {
    Timber.d { "Using currently active network for proxy connections..." }
    block(NOOP_BOUND)
  }

  companion object {
    private val NOOP_BOUND =
        object : SocketBinder.NetworkBinder {
          override suspend fun bindToNetwork(socket: Socket) {
            // Do nothing
          }

          override suspend fun bindToNetwork(datagramSocket: DatagramSocket) {
            // Do nothing
          }
        }
  }
}
