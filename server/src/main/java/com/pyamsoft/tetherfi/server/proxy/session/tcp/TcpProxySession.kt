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
import com.pyamsoft.tetherfi.server.ServerSocketTimeout
import com.pyamsoft.tetherfi.server.SocketCreator
import com.pyamsoft.tetherfi.server.broadcast.BroadcastNetworkStatus
import com.pyamsoft.tetherfi.server.clients.AllowedClients
import com.pyamsoft.tetherfi.server.clients.BlockedClients
import com.pyamsoft.tetherfi.server.clients.ByteTransferReport
import com.pyamsoft.tetherfi.server.clients.ClientResolver
import com.pyamsoft.tetherfi.server.clients.TetherClient
import com.pyamsoft.tetherfi.server.network.SocketBinder
import com.pyamsoft.tetherfi.server.proxy.ServerDispatcher
import com.pyamsoft.tetherfi.server.proxy.SharedProxy
import com.pyamsoft.tetherfi.server.proxy.SocketTagger
import com.pyamsoft.tetherfi.server.proxy.SocketTracker
import com.pyamsoft.tetherfi.server.proxy.session.ProxySession
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal abstract class TcpProxySession<Q : ProxyRequest>
protected constructor(
    private val transport: TcpSessionTransport<Q>,
    private val blockedClients: BlockedClients,
    private val clientResolver: ClientResolver,
    private val allowedClients: AllowedClients,
    protected val socketTagger: SocketTagger,
    protected val enforcer: ThreadEnforcer,
) : ProxySession<TcpProxyData> {

  private val logTag: String by lazy { proxyType.name }

  private fun CoroutineScope.handleClientRequestSideEffects(
      serverDispatcher: ServerDispatcher,
      client: TetherClient,
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
    launch(context = serverDispatcher.sideEffect) { allowedClients.seen(client) }
  }

  private fun CoroutineScope.handleClientReportSideEffects(
      serverDispatcher: ServerDispatcher,
      report: ByteTransferReport,
      client: TetherClient,
  ) {
    enforcer.assertOffMainThread()

    // Track the report for the given client
    launch(context = serverDispatcher.sideEffect) { allowedClients.reportTransfer(client, report) }
  }

  @CheckResult
  private fun resolveClientOrBlock(
      hostConnection: BroadcastNetworkStatus.ConnectionInfo.Connected,
      hostNameOrIp: String
  ): TetherClient? {
    // If the host is an IP address, and we are an IP address,
    // check that we fall into the host
    if (hostConnection.isIpAddress) {
      if (IP_ADDRESS_REGEX.matches(hostNameOrIp)) {
        if (!hostConnection.isClientWithinAddressableIpRange(hostNameOrIp)) {
          warnLog { "Reject IP address outside of host range: $hostNameOrIp" }
          return null
        }
      }
    }

    // Retrieve the client (or track if it is it new)
    val client = clientResolver.ensure(hostNameOrIp)

    // If the client is blocked we do not process any input
    if (blockedClients.isBlocked(client)) {
      warnLog { "Client is marked blocked: $client" }
      return null
    }

    return client
  }

  private suspend fun processRequest(
      scope: CoroutineScope,
      socketCreator: SocketCreator,
      timeout: ServerSocketTimeout,
      connectionInfo: BroadcastNetworkStatus.ConnectionInfo.Connected,
      networkBinder: SocketBinder.NetworkBinder,
      serverDispatcher: ServerDispatcher,
      proxyInput: ByteReadChannel,
      proxyOutput: ByteWriteChannel,
      socketTracker: SocketTracker,
      client: TetherClient,
      request: Q,
  ) {
    // This is launched as its own scope so that the side effect does not slow
    // down the internet traffic processing.
    // Since this context is our own dispatcher which is cachedThreadPool backed,
    // we just "spin up" another thread and forget about it performance wise.
    scope.launch(context = serverDispatcher.primary) {
      handleClientRequestSideEffects(
          serverDispatcher = serverDispatcher,
          client = client,
      )
    }

    // And then we go to the web!
    proxyToInternet(
        scope = scope,
        socketCreator = socketCreator,
        timeout = timeout,
        connectionInfo = connectionInfo,
        networkBinder = networkBinder,
        serverDispatcher = serverDispatcher,
        proxyInput = proxyInput,
        proxyOutput = proxyOutput,
        socketTracker = socketTracker,
        request = request,
        client = client,
        onReport = { report ->
          // This is launched as its own scope so that the side effect does not slow
          // down the internet traffic processing.
          // Since this context is our own dispatcher which is cachedThreadPool backed,
          // we just "spin up" another thread and forget about it performance wise.
          scope.launch(context = serverDispatcher.primary) {
            handleClientReportSideEffects(
                serverDispatcher = serverDispatcher,
                report = report,
                client = client,
            )
          }
        },
    )
  }

  private suspend fun handleClientRequest(
      scope: CoroutineScope,
      socketCreator: SocketCreator,
      timeout: ServerSocketTimeout,
      networkBinder: SocketBinder.NetworkBinder,
      hostConnection: BroadcastNetworkStatus.ConnectionInfo.Connected,
      serverDispatcher: ServerDispatcher,
      proxyInput: ByteReadChannel,
      proxyOutput: ByteWriteChannel,
      socketTracker: SocketTracker,
      hostNameOrIp: String,
  ) {

    // We use a string parsing to figure out what this HTTP request wants to do
    // Inline to avoid new object allocation
    val request: Q = transport.parseRequest(proxyInput, proxyOutput)
    if (!request.valid) {
      warnLog { "Could not parse proxy request $request" }
      transport.writeProxyOutput(proxyOutput, request, TransportWriteCommand.INVALID)
      return
    }

    val client = resolveClientOrBlock(hostConnection, hostNameOrIp)
    if (client == null) {
      transport.writeProxyOutput(proxyOutput, request, TransportWriteCommand.BLOCK)
      return
    }

    processRequest(
        scope = scope,
        socketCreator = socketCreator,
        timeout = timeout,
        connectionInfo = hostConnection,
        networkBinder = networkBinder,
        serverDispatcher = serverDispatcher,
        proxyInput = proxyInput,
        proxyOutput = proxyOutput,
        socketTracker = socketTracker,
        client = client,
        request = request,
    )
  }

  protected inline fun debugLog(message: () -> String) {
    Timber.d { "$logTag: ${message()}" }
  }

  protected inline fun warnLog(message: () -> String) {
    Timber.w { "$logTag: ${message()}" }
  }

  protected inline fun errorLog(throwable: Throwable, message: () -> String) {
    Timber.e(throwable) { "$logTag: ${message()}" }
  }

  override suspend fun exchange(
      scope: CoroutineScope,
      socketCreator: SocketCreator,
      timeout: ServerSocketTimeout,
      networkBinder: SocketBinder.NetworkBinder,
      hostConnection: BroadcastNetworkStatus.ConnectionInfo.Connected,
      serverDispatcher: ServerDispatcher,
      socketTracker: SocketTracker,
      data: TcpProxyData,
  ) =
      withContext(context = serverDispatcher.primary) {
        val proxyInput = data.proxyInput
        val proxyOutput = data.proxyOutput
        val hostNameOrIp = data.hostNameOrIp
        try {
          handleClientRequest(
              scope = this,
              socketCreator = socketCreator,
              timeout = timeout,
              networkBinder = networkBinder,
              hostConnection = hostConnection,
              serverDispatcher = serverDispatcher,
              proxyInput = proxyInput,
              proxyOutput = proxyOutput,
              hostNameOrIp = hostNameOrIp,
              socketTracker = socketTracker,
          )
        } catch (e: Throwable) {
          e.ifNotCancellation { errorLog(e) { "Error handling client Request: $hostNameOrIp" } }
        }
      }

  protected abstract suspend fun proxyToInternet(
      scope: CoroutineScope,
      socketCreator: SocketCreator,
      timeout: ServerSocketTimeout,
      connectionInfo: BroadcastNetworkStatus.ConnectionInfo.Connected,
      networkBinder: SocketBinder.NetworkBinder,
      serverDispatcher: ServerDispatcher,
      proxyInput: ByteReadChannel,
      proxyOutput: ByteWriteChannel,
      socketTracker: SocketTracker,
      client: TetherClient,
      request: Q,
      onReport: suspend (ByteTransferReport) -> Unit,
  )

  protected abstract val proxyType: SharedProxy.Type
}
