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

package com.pyamsoft.tetherfi.server.proxy.session.tcp.socks

import com.pyamsoft.tetherfi.server.ServerSocketTimeout
import com.pyamsoft.tetherfi.server.SocketCreator
import com.pyamsoft.tetherfi.server.broadcast.BroadcastNetworkStatus
import com.pyamsoft.tetherfi.server.clients.ByteTransferReport
import com.pyamsoft.tetherfi.server.clients.TetherClient
import com.pyamsoft.tetherfi.server.network.SocketBinder
import com.pyamsoft.tetherfi.server.proxy.ProxyConnectionInfo
import com.pyamsoft.tetherfi.server.proxy.ServerDispatcher
import com.pyamsoft.tetherfi.server.proxy.SharedProxy
import com.pyamsoft.tetherfi.server.proxy.SocketTracker
import com.pyamsoft.tetherfi.server.proxy.session.tcp.AbstractTcpSessionTransport
import com.pyamsoft.tetherfi.server.proxy.session.tcp.TransportWriteCommand
import com.pyamsoft.tetherfi.server.proxy.session.tcp.socks.five.SOCKS5Implementation
import com.pyamsoft.tetherfi.server.proxy.session.tcp.socks.four.SOCKS4Implementation
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.readByte
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext

@Singleton
internal class SOCKSTransport
@Inject
internal constructor(
    private val socks4: SOCKS4Implementation,
    private val socks5: SOCKS5Implementation,
) : AbstractTcpSessionTransport<SOCKSVersion>() {

  override val proxyType = SharedProxy.Type.SOCKS

  override suspend fun writeProxyOutput(
      output: ByteWriteChannel,
      request: SOCKSVersion,
      command: TransportWriteCommand
  ) =
      when (request) {
        SOCKSVersion.Invalid -> {
          warnLog { "Asked to write proxy output to INVALID SOCKS version: $command" }
        }
        SOCKSVersion.SOCKS4 -> {
          when (command) {
            // All the same command for SOCKS4
            TransportWriteCommand.INVALID -> socks4.usingResponder(output) { sendRefusal() }
            TransportWriteCommand.BLOCK -> socks4.usingResponder(output) { sendRefusal() }
            TransportWriteCommand.ERROR -> socks4.usingResponder(output) { sendRefusal() }
          }
        }
        SOCKSVersion.SOCKS5 -> {
          when (command) {
            TransportWriteCommand.INVALID -> socks5.usingResponder(output) { sendRefusal() }
            TransportWriteCommand.BLOCK -> socks5.usingResponder(output) { sendRefusal() }
            TransportWriteCommand.ERROR -> socks5.usingResponder(output) { sendError() }
          }
        }
      }

  override suspend fun parseRequest(
      input: ByteReadChannel,
      output: ByteWriteChannel
  ): SOCKSVersion {
    try {
      val versionByte = input.readByte()
      return SOCKSVersion.fromVersion(versionByte)
    } catch (e: Throwable) {
      errorLog(e) { "Error reading initial input byte for SOCKS version" }
      return SOCKSVersion.Invalid
    }
  }

  suspend fun handleRequest(
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
      version: SOCKSVersion,
      onReport: suspend (ByteTransferReport) -> Unit
  ) =
      withContext(context = serverDispatcher.primary) {
        when (version) {
          SOCKSVersion.Invalid -> {
            warnLog { "Invalid SOCKS version, can't handle this request!" }
          }
          SOCKSVersion.SOCKS4 -> {
            socks4.handleSocksCommand(
                scope = scope,
                socketCreator = socketCreator,
                timeout = timeout,
                serverDispatcher = serverDispatcher,
                socketTracker = socketTracker,
                networkBinder = networkBinder,
                proxyInput = proxyInput,
                proxyOutput = proxyOutput,
                proxyConnectionInfo = proxyConnectionInfo,
                connectionInfo = connectionInfo,
                client = client,
                onReport = onReport,
            )
          }
          SOCKSVersion.SOCKS5 -> {
            socks5.handleSocksCommand(
                scope = scope,
                socketCreator = socketCreator,
                timeout = timeout,
                serverDispatcher = serverDispatcher,
                socketTracker = socketTracker,
                networkBinder = networkBinder,
                proxyInput = proxyInput,
                proxyOutput = proxyOutput,
                proxyConnectionInfo = proxyConnectionInfo,
                connectionInfo = connectionInfo,
                client = client,
                onReport = onReport,
            )
          }
        }
      }
}
