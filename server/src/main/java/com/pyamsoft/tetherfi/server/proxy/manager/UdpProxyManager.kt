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
import com.pyamsoft.tetherfi.server.proxy.session.udp.UdpProxyData
import io.ktor.network.sockets.BoundDatagramSocket
import io.ktor.network.sockets.Datagram
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.SocketAddress
import io.ktor.network.sockets.SocketBuilder
import io.ktor.network.sockets.isClosed
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

internal class UdpProxyManager
internal constructor(
    private val dispatcher: CoroutineDispatcher,
    private val enforcer: ThreadEnforcer,
    private val session: ProxySession<UdpProxyData>,
) :
    BaseProxyManager<BoundDatagramSocket>(
        dispatcher = dispatcher,
        enforcer = enforcer,
    ) {

  @CheckResult
  private fun getServerAddress(): SocketAddress {
    return InetSocketAddress(
        hostname = "0.0.0.0",
        port = 0,
    )
  }

  private suspend fun runSession(
      scope: CoroutineScope,
      datagram: Datagram,
  ) {
    enforcer.assertOffMainThread()

    try {
      session.exchange(
          scope = scope,
          data =
              UdpProxyData(
                  datagram = datagram,
              ),
      )
    } catch (e: Throwable) {
      e.ifNotCancellation { Timber.e(e, "Error during session $datagram") }
    }
  }

  override suspend fun openServer(builder: SocketBuilder): BoundDatagramSocket =
      withContext(context = dispatcher) {
        enforcer.assertOffMainThread()

        val localAddress = getServerAddress()
        Timber.d("Bind UDP server to local address: $localAddress")

        return@withContext builder.udp().bind(localAddress = localAddress)
      }

  override suspend fun runServer(server: BoundDatagramSocket) =
      withContext(context = dispatcher) {
        enforcer.assertOffMainThread()

        Timber.d("Awaiting UDP connections on ${server.localAddress}")

        // In a loop, we wait for new TCP connections and then offload them to their own routine.
        while (isActive && !server.isClosed) {
          // We must close the connection in the launch{} after exchange is over
          val datagram = server.receive()

          if (isActive || server.isClosed) {
            // Run this server loop off thread so we can handle multiple connections at once.
            launch(context = dispatcher) { runSession(this, datagram) }
          } else {
            // Immediately drop the connection
            Timber.w("Server is closed, immediately drop connection")
          }
        }
      }
}
