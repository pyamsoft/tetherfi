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

package com.pyamsoft.tetherfi.server.proxy.session.tcp.socks

import com.pyamsoft.pydroid.core.ThreadEnforcer
import com.pyamsoft.pydroid.util.ifNotCancellation
import com.pyamsoft.tetherfi.server.ServerSocketTimeout
import com.pyamsoft.tetherfi.server.SocketCreator
import com.pyamsoft.tetherfi.server.broadcast.BroadcastNetworkStatus
import com.pyamsoft.tetherfi.server.clients.AllowedClients
import com.pyamsoft.tetherfi.server.clients.BlockedClients
import com.pyamsoft.tetherfi.server.clients.ByteTransferReport
import com.pyamsoft.tetherfi.server.clients.ClientResolver
import com.pyamsoft.tetherfi.server.clients.TetherClient
import com.pyamsoft.tetherfi.server.network.SocketBinder
import com.pyamsoft.tetherfi.server.proxy.ProxyConnectionInfo
import com.pyamsoft.tetherfi.server.proxy.ServerDispatcher
import com.pyamsoft.tetherfi.server.proxy.SharedProxy
import com.pyamsoft.tetherfi.server.proxy.SocketTagger
import com.pyamsoft.tetherfi.server.proxy.SocketTracker
import com.pyamsoft.tetherfi.server.proxy.session.tcp.TcpProxySession
import com.pyamsoft.tetherfi.server.proxy.session.tcp.TransportWriteCommand
import io.ktor.network.sockets.SocketTimeoutException
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope

@Singleton
internal class SOCKSProxySession
@Inject
internal constructor(
    private val transport: SOCKSTransport,
    socketTagger: SocketTagger,
    blockedClients: BlockedClients,
    clientResolver: ClientResolver,
    allowedClients: AllowedClients,
    enforcer: ThreadEnforcer,
) :
    TcpProxySession<SOCKSVersion>(
        blockedClients = blockedClients,
        clientResolver = clientResolver,
        allowedClients = allowedClients,
        socketTagger = socketTagger,
        enforcer = enforcer,
        transport = transport,
    ) {

  private suspend fun handleProxyToInternetError(
      throwable: Throwable,
      client: TetherClient,
      request: SOCKSVersion,
      proxyOutput: ByteWriteChannel,
  ) {
    throwable.ifNotCancellation {
      // Generally, the Transport should handle SocketTimeoutException itself.
      // We capture here JUST in case
      if (throwable is SocketTimeoutException) {
        warnLog { "Proxy:Internet socket timeout! $request $client" }
      } else {
        errorLog(throwable) { "Error during Internet exchange $request $client" }
        transport.writeProxyOutput(proxyOutput, request, TransportWriteCommand.ERROR)
      }
    }
  }

  override val proxyType = SharedProxy.Type.SOCKS

  override suspend fun proxyToInternet(
      scope: CoroutineScope,
      socketCreator: SocketCreator,
      timeout: ServerSocketTimeout,
      connectionInfo: BroadcastNetworkStatus.ConnectionInfo.Connected,
      networkBinder: SocketBinder.NetworkBinder,
      serverDispatcher: ServerDispatcher,
      proxyInput: ByteReadChannel,
      proxyOutput: ByteWriteChannel,
      proxyConnectionInfo: ProxyConnectionInfo,
      socketTracker: SocketTracker,
      client: TetherClient,
      request: SOCKSVersion,
      onReport: suspend (ByteTransferReport) -> Unit
  ) {
    enforcer.assertOffMainThread()

    // Given the request, connect to the Web
    try {
      transport.handleRequest(
          scope = scope,
          socketCreator = socketCreator,
          timeout = timeout,
          connectionInfo = connectionInfo,
          serverDispatcher = serverDispatcher,
          proxyInput = proxyInput,
          proxyOutput = proxyOutput,
          proxyConnectionInfo = proxyConnectionInfo,
          socketTracker = socketTracker,
          networkBinder = networkBinder,
          client = client,
          version = request,
          onError = {
            handleProxyToInternetError(
                throwable = it,
                client = client,
                request = request,
                proxyOutput = proxyOutput,
            )
          },
          onReport = onReport,
      )
    } catch (e: Throwable) {
      handleProxyToInternetError(
          throwable = e,
          client = client,
          request = request,
          proxyOutput = proxyOutput,
      )
    }
  }
}
