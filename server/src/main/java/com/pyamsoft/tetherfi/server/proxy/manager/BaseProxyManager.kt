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
import com.pyamsoft.tetherfi.server.proxy.usingSocket
import io.ktor.network.sockets.ASocket
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.SocketAddress
import io.ktor.network.sockets.SocketBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import timber.log.Timber

internal abstract class BaseProxyManager<S : ASocket> protected constructor() : ProxyManager {

  override suspend fun loop() =
      withContext(context = Dispatchers.IO) {
        return@withContext usingSocket { socket ->
          val server = openServer(builder = socket)
          try {
            runServer(server)
          } finally {
            withContext(context = NonCancellable) { server.dispose() }
          }
        }
      }

  protected abstract suspend fun runServer(server: S)

  @CheckResult protected abstract suspend fun openServer(builder: SocketBuilder): S

  companion object {

    @JvmStatic
    @CheckResult
    protected fun getServerAddress(
        hostName: String,
        port: Int,
        verifyPort: Boolean,
        verifyHostName: Boolean,
    ): SocketAddress {
      // Port must be in the valid range
      if (verifyPort) {
        if (port > 65000 || port <= 1024) {
          val err = "Port is invalid: $port"
          Timber.w(err)
          throw IllegalArgumentException(err)
        }
      }

      if (verifyHostName) {
        // Name must be valid
        if (hostName.isBlank()) {
          val err = "HostName is invalid: $hostName"
          Timber.w(err)
          throw IllegalArgumentException(err)
        }
      }

      return InetSocketAddress(
          hostname = hostName,
          port = port,
      )
    }
  }
}
