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

package com.pyamsoft.tetherfi.server.proxy.session.tcp

import androidx.annotation.CheckResult
import com.pyamsoft.pydroid.core.ThreadEnforcer
import com.pyamsoft.pydroid.util.ifNotCancellation
import com.pyamsoft.tetherfi.core.Timber
import com.pyamsoft.tetherfi.server.IP_ADDRESS_REGEX
import com.pyamsoft.tetherfi.server.ServerInternalApi
import com.pyamsoft.tetherfi.server.broadcast.BroadcastNetworkStatus
import com.pyamsoft.tetherfi.server.clients.AllowedClients
import com.pyamsoft.tetherfi.server.clients.BlockedClients
import com.pyamsoft.tetherfi.server.clients.ByteTransferReport
import com.pyamsoft.tetherfi.server.event.ProxyRequest
import com.pyamsoft.tetherfi.server.proxy.ServerDispatcher
import com.pyamsoft.tetherfi.server.proxy.session.ProxySession
import com.pyamsoft.tetherfi.server.proxy.session.tagSocket
import com.pyamsoft.tetherfi.server.proxy.usingConnection
import com.pyamsoft.tetherfi.server.proxy.usingSocketBuilder
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class TcpProxySession
@Inject
internal constructor(
    /** Need to use MutableSet instead of Set because of Java -> Kotlin fun. */
    @ServerInternalApi private val transports: MutableSet<TcpSessionTransport>,
    private val blockedClients: BlockedClients,
    private val allowedClients: AllowedClients,
    private val enforcer: ThreadEnforcer,
) : ProxySession<TcpProxyData> {

  /**
   * Given the initial proxy request, connect to the Internet from our device via the connected
   * socket
   */
  private suspend inline fun <T> connectToInternet(
      autoFlush: Boolean,
      serverDispatcher: ServerDispatcher,
      request: ProxyRequest,
      block: (ByteReadChannel, ByteWriteChannel) -> T
  ): T {
    enforcer.assertOffMainThread()

    return usingSocketBuilder(serverDispatcher.primary) { builder ->
      tagSocket()

      // We dont actually use the socket tls() method here since we are not a TLS server
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
                reusePort = true
              }
              .connect(remoteAddress = remote)

      return@usingSocketBuilder socket.usingConnection(autoFlush = autoFlush, block)
    }
  }

  private fun CoroutineScope.handleClientRequestSideEffects(
      serverDispatcher: ServerDispatcher,
      hostNameOrIp: String,
  ) {
    enforcer.assertOffMainThread()

    // Mark all client connections as seen
    //
    // We need to do this because we have access to the MAC address via the GroupInfo.clientList
    // but not the IP address. Android does not let us access the system ARP table so we cannot map
    // MACs to IPs. Thus we need to basically hold our own table of "known" IP addresses and allow
    // a user to block them as they see fit. This is UX wise, not great at all, since a user must
    // eliminate a "bad" IP address by first knowing all the good ones.
    //
    // Though, arguably, blocking is only a nice to have. Real network security should be handled
    // via the password.
    launch(context = serverDispatcher.sideEffect) { allowedClients.seen(hostNameOrIp) }
  }

  private fun CoroutineScope.handleClientReportSideEffects(
      serverDispatcher: ServerDispatcher,
      hostNameOrIp: String,
      report: ByteTransferReport,
  ) {
    enforcer.assertOffMainThread()

    // Track the report for the given client
    launch(context = serverDispatcher.sideEffect) {
      allowedClients.reportTransfer(hostNameOrIp, report)
    }
  }

  @CheckResult
  private suspend fun CoroutineScope.proxyToInternet(
      serverDispatcher: ServerDispatcher,
      handler: RequestHandler,
      proxyInput: ByteReadChannel,
      proxyOutput: ByteWriteChannel,
  ): ByteTransferReport? {
    enforcer.assertOffMainThread()

    val request = handler.request
    val transport = handler.transport

    // Given the request, connect to the Web
    try {
      val report: ByteTransferReport? =
          connectToInternet(
              autoFlush = true,
              serverDispatcher = serverDispatcher,
              request = request,
          ) { internetInput, internetOutput ->
            // Communicate between the web connection we've made and back to our client device
            transport.exchangeInternet(
                scope = this,
                serverDispatcher = serverDispatcher,
                proxyInput = proxyInput,
                proxyOutput = proxyOutput,
                internetInput = internetInput,
                internetOutput = internetOutput,
                request = request,
            )
          }

      return report
    } catch (e: Throwable) {
      e.ifNotCancellation {
        Timber.e(e) { "Error during Internet exchange" }
        writeError(proxyOutput)
      }
      return null
    }
  }

  private suspend fun handleClientRequest(
      scope: CoroutineScope,
      hostConnection: BroadcastNetworkStatus.ConnectionInfo.Connected,
      serverDispatcher: ServerDispatcher,
      proxyInput: ByteReadChannel,
      proxyOutput: ByteWriteChannel,
      hostNameOrIp: String,
  ) {
    // If the host is an IP address, and we are an IP address,
    // check that we fall into the host
    if (hostConnection.isIpAddress) {
      if (IP_ADDRESS_REGEX.matches(hostNameOrIp)) {
        if (!hostConnection.isClientWithinAddressableIpRange(hostNameOrIp)) {
          Timber.w { "Reject IP address outside of host range: $hostNameOrIp" }
          writeError(proxyOutput)
          return
        }
      }
    }

    // If the client is blocked we do not process any inpue
    if (blockedClients.isBlocked(hostNameOrIp)) {
      Timber.w { "Client is marked blocked: $hostNameOrIp" }
      writeError(proxyOutput)
      return
    }

    // This is launched as its own scope so that the side effect does not slow
    // down the internet traffic processing.
    // Since this context is our own dispatcher which is cachedThreadPool backed,
    // we just "spin up" another thread and forget about it performance wise.
    scope.launch(context = serverDispatcher.primary) {
      handleClientRequestSideEffects(
          serverDispatcher = serverDispatcher,
          hostNameOrIp = hostNameOrIp,
      )
    }

    // We use a string parsing to figure out what this HTTP request wants to do
    val handler =
        transports.firstNotNullOfOrNull { transport ->
          val req = transport.parseRequest(proxyInput)
          if (req == null) {
            return@firstNotNullOfOrNull null
          } else {
            return@firstNotNullOfOrNull RequestHandler(
                transport = transport,
                request = req,
            )
          }
        }
    if (handler == null) {
      Timber.w { "Could not parse proxy request" }
      writeError(proxyOutput)
      return
    }

    // And then we go to the web!
    val report =
        scope.proxyToInternet(
            serverDispatcher = serverDispatcher,
            handler = handler,
            proxyInput = proxyInput,
            proxyOutput = proxyOutput,
        )

    if (report != null) {
      // This is launched as its own scope so that the side effect does not slow
      // down the internet traffic processing.
      // Since this context is our own dispatcher which is cachedThreadPool backed,
      // we just "spin up" another thread and forget about it performance wise.
      scope.launch(context = serverDispatcher.primary) {
        handleClientReportSideEffects(
            serverDispatcher = serverDispatcher,
            hostNameOrIp = hostNameOrIp,
            report = report,
        )
      }
    }
  }

  override suspend fun exchange(
      scope: CoroutineScope,
      hostConnection: BroadcastNetworkStatus.ConnectionInfo.Connected,
      serverDispatcher: ServerDispatcher,
      data: TcpProxyData,
  ) =
      withContext(context = serverDispatcher.primary) {
        val proxyInput = data.proxyInput
        val proxyOutput = data.proxyOutput
        val hostNameOrIp = data.hostNameOrIp
        try {
          handleClientRequest(
              scope = this,
              hostConnection = hostConnection,
              serverDispatcher = serverDispatcher,
              proxyInput = proxyInput,
              proxyOutput = proxyOutput,
              hostNameOrIp = hostNameOrIp,
          )
        } catch (e: Throwable) {
          e.ifNotCancellation { Timber.e(e) { "Error during Internet exchange" } }
        }
      }

  private data class RequestHandler(
      val transport: TcpSessionTransport,
      val request: ProxyRequest,
  )
}
