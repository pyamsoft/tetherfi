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
import com.pyamsoft.pydroid.util.ifNotCancellation
import com.pyamsoft.tetherfi.server.proxy.session.ProxySession
import com.pyamsoft.tetherfi.server.proxy.session.tcp.TcpProxyData
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.ServerSocket
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.SocketAddress
import io.ktor.network.sockets.SocketBuilder
import io.ktor.network.sockets.isClosed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

internal class TcpProxyManager
internal constructor(
    private val enforcer: ThreadEnforcer,
    private val session: ProxySession<TcpProxyData>,
    private val port: Int,
) : BaseProxyManager<ServerSocket>() {

  @CheckResult
  private fun getServerAddress(port: Int): SocketAddress {
    return InetSocketAddress(
        hostname = "0.0.0.0",
        port = port,
    )
  }

  private suspend fun runSession(
      scope: CoroutineScope,
      connection: Socket,
  ) {
    enforcer.assertOffMainThread()

    try {
      session.exchange(
          scope = scope,
          data =
              TcpProxyData(
                  connection = connection,
              ),
      )
    } catch (e: Throwable) {
      e.ifNotCancellation { Timber.e(e, "Error during session $connection") }
    }
  }

  override suspend fun openServer(builder: SocketBuilder): ServerSocket =
      withContext(context = Dispatchers.IO) {
        // Port must be in the valid range
        if (port > 65000 || port <= 1024) {
          val err = "Port is invalid: $port"
          Timber.w(err)
          throw IllegalArgumentException(err)
        }

        val localAddress = getServerAddress(port)
        Timber.d("Bind TCP server to local address: $localAddress")

        return@withContext builder.tcp().bind(localAddress = localAddress)
      }

  override suspend fun runServer(server: ServerSocket) =
      withContext(context = Dispatchers.IO) {
        Timber.d("Awaiting TCP connections on ${server.localAddress}")

        // In a loop, we wait for new TCP connections and then offload them to their own routine.
        while (isActive && !server.isClosed) {
          // We must close the connection in the launch{} after exchange is over
          val connection = server.accept()

          // Run this server loop off thread so we can handle multiple connections at once.
          launch(context = Dispatchers.IO) {
            try {
              runSession(this, connection)
            } finally {
              withContext(context = NonCancellable) { connection.dispose() }
            }
          }
        }
      }
}
