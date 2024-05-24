/*
 * Copyright 2024 pyamsoft
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

package com.pyamsoft.tetherfi.server.proxy.manager

import com.pyamsoft.pydroid.core.ThreadEnforcer
import com.pyamsoft.pydroid.util.ifNotCancellation
import com.pyamsoft.tetherfi.core.Timber
import com.pyamsoft.tetherfi.server.broadcast.BroadcastNetworkStatus
import com.pyamsoft.tetherfi.server.proxy.ServerDispatcher
import com.pyamsoft.tetherfi.server.proxy.SocketTagger
import com.pyamsoft.tetherfi.server.proxy.session.ProxySession
import com.pyamsoft.tetherfi.server.proxy.session.udp.UdpProxyData
import io.ktor.network.sockets.BoundDatagramSocket
import io.ktor.network.sockets.Datagram
import io.ktor.network.sockets.SocketBuilder
import io.ktor.network.sockets.isClosed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class UdpProxyManager
internal constructor(
    private val enforcer: ThreadEnforcer,
    private val socketTagger: SocketTagger,
    private val session: ProxySession<UdpProxyData>,
    private val hostConnection: BroadcastNetworkStatus.ConnectionInfo.Connected,
    private val port: Int,
    serverDispatcher: ServerDispatcher,
) :
    BaseProxyManager<BoundDatagramSocket>(
        serverDispatcher = serverDispatcher,
    ) {

  private suspend fun runSession(
      scope: CoroutineScope,
      datagram: Datagram,
  ) {
    enforcer.assertOffMainThread()

    try {
      session.exchange(
          scope = scope,
          hostConnection = hostConnection,
          serverDispatcher = serverDispatcher,
          data =
              UdpProxyData(
                  datagram = datagram,
              ),
      )
    } catch (e: Throwable) {
      e.ifNotCancellation { Timber.e(e) { "Error during session $datagram" } }
    }
  }

  override suspend fun openServer(builder: SocketBuilder): BoundDatagramSocket =
      withContext(context = serverDispatcher.primary) {
        val localAddress =
            getServerAddress(
                hostName = hostConnection.hostName,
                port = port,
                verifyPort = false,
                verifyHostName = true,
            )
        Timber.d { "Bind UDP server to local address: $localAddress" }
        return@withContext builder
            .udp()
            .configure {
              reuseAddress = true
              reusePort = true
            }
            .also { socketTagger.tagSocket() }
            .bind(localAddress = localAddress)
      }

  override suspend fun runServer(server: BoundDatagramSocket) =
      withContext(context = serverDispatcher.primary) {
        Timber.d { "Awaiting UDP connections on ${server.localAddress}" }

        // In a loop, we wait for new TCP connections and then offload them to their own routine.
        while (!server.isClosed) {
          // We must close the connection in the launch{} after exchange is over
          val datagram = server.receive()

          if (server.isClosed) {
            // Run this server loop off thread so we can handle multiple connections at once.
            launch(context = serverDispatcher.primary) { runSession(this, datagram) }
          } else {
            // Immediately drop the connection
            Timber.w { "Server is closed, immediately drop connection" }
          }
        }
      }
}
