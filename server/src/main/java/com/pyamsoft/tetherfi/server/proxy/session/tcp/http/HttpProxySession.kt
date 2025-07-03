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

package com.pyamsoft.tetherfi.server.proxy.session.tcp.http

import com.pyamsoft.pydroid.core.ThreadEnforcer
import com.pyamsoft.pydroid.util.ifNotCancellation
import com.pyamsoft.tetherfi.core.notification.NotificationErrorLauncher
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
import com.pyamsoft.tetherfi.server.proxy.usingConnection
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.SocketTimeoutException
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Singleton
internal class HttpProxySession
@Inject
internal constructor(
    @param:Named("app_scope") private val appScope: CoroutineScope,
    private val notificationErrorLauncher: NotificationErrorLauncher,
    private val transport: HttpTransport,
    socketTagger: SocketTagger,
    blockedClients: BlockedClients,
    clientResolver: ClientResolver,
    allowedClients: AllowedClients,
    enforcer: ThreadEnforcer,
) :
    TcpProxySession<HttpProxyRequest>(
        transport = transport,
        socketTagger = socketTagger,
        blockedClients = blockedClients,
        clientResolver = clientResolver,
        allowedClients = allowedClients,
        enforcer = enforcer,
    ) {

  override val proxyType = SharedProxy.Type.HTTP

  /**
   * Given the initial proxy request, connect to the Internet from our device via the connected
   * socket
   *
   * This function must ALWAYS call connection.usingConnection {} or else a socket may potentially
   * leak
   */
  private suspend inline fun <T> connectToInternet(
      networkBinder: SocketBinder.NetworkBinder,
      socketCreator: SocketCreator,
      timeout: ServerSocketTimeout,
      autoFlush: Boolean,
      request: HttpProxyRequest,
      socketTracker: SocketTracker,
      noinline onError: (Throwable) -> Unit,
      crossinline block: suspend (ByteReadChannel, ByteWriteChannel) -> T
  ): T =
      socketCreator.create(
          type = SocketCreator.Type.CLIENT,
          onError = { onError },
          onBuild = { builder ->
            // We don't actually use the socket tls() method here since we are not a TLS server
            // We do the CONNECT based workaround to handle HTTPS connections
            val remote =
                InetSocketAddress(
                    hostname = request.host,
                    port = request.port,
                )

            val socket =
                builder
                    .tcp()
                    .configure {
                      reuseAddress = true
                      // As of KTOR-3.0.0, this is not supported and crashes at runtime
                      // reusePort = true
                    }
                    .also { socketTagger.tagSocket() }
                    // This function uses our custom build of KTOR
                    // which adds the [onBeforeConnect] hook to allow us
                    // to use the socket created BEFORE connection starts.
                    .connectWithConfiguration(
                        remoteAddress = remote,
                        configure = {
                          // By default KTOR does not close sockets until "infinity" is reached.
                          val duration = timeout.timeoutDuration
                          if (!duration.isInfinite()) {
                            socketTimeout = duration.inWholeMilliseconds
                          }
                        },
                        onBeforeConnect = { networkBinder.bindToNetwork(it) },
                    )

            // Track this socket for when we fully shut down
            socketTracker.track(socket)

            return@create socket.usingConnection(autoFlush = autoFlush) {
                internetInput,
                internetOutput ->
              block(internetInput, internetOutput)
            }
          },
      )

  private suspend fun handleProxyToInternetError(
      throwable: Throwable,
      client: TetherClient,
      request: HttpProxyRequest,
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
      request: HttpProxyRequest,
      onReport: suspend (ByteTransferReport) -> Unit
  ) {
    enforcer.assertOffMainThread()

    // Given the request, connect to the Web
    try {
      connectToInternet(
          autoFlush = true,
          socketCreator = socketCreator,
          timeout = timeout,
          networkBinder = networkBinder,
          socketTracker = socketTracker,
          request = request,
          onError = { e ->
            // This error comes from the SelectorManager launch {} scope,
            // so everything may be dead. fallback to Dispatchers.IO since we cannot be guaranteed
            // that
            // our custom dispatcher pool is around
            appScope.launch(context = Dispatchers.IO) {
              // Handle the error by killing the connection
              handleProxyToInternetError(
                  throwable = e,
                  proxyOutput = proxyOutput,
                  request = request,
                  client = client,
              )

              // Also inform the user via error notification
              // but do NOT shut down the hotspot
              //
              // Since this could potentially fire a lot, just update the notification to the latest
              // one
              notificationErrorLauncher.showError(e)
            }
          },
          block = { internetInput, internetOutput ->
            try {
              // Communicate between the web connection we've made and back to our client device
              transport.exchangeInternet(
                  scope = scope,
                  serverDispatcher = serverDispatcher,
                  proxyInput = proxyInput,
                  proxyOutput = proxyOutput,
                  internetInput = internetInput,
                  internetOutput = internetOutput,
                  request = request,
                  client = client,
                  onReport = onReport,
              )
            } finally {
              internetOutput.flush()
            }
          },
      )
    } catch (e: Throwable) {
      handleProxyToInternetError(
          throwable = e,
          proxyOutput = proxyOutput,
          request = request,
          client = client,
      )
    }
  }
}
