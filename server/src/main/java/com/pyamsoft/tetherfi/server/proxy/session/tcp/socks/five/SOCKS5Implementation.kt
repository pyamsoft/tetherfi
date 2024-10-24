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

package com.pyamsoft.tetherfi.server.proxy.session.tcp.socks.five

import androidx.annotation.CheckResult
import com.pyamsoft.tetherfi.core.Timber
import com.pyamsoft.tetherfi.server.broadcast.BroadcastNetworkStatus
import com.pyamsoft.tetherfi.server.clients.ByteTransferReport
import com.pyamsoft.tetherfi.server.clients.TetherClient
import com.pyamsoft.tetherfi.server.network.SocketBinder
import com.pyamsoft.tetherfi.server.proxy.ServerDispatcher
import com.pyamsoft.tetherfi.server.proxy.SocketTagger
import com.pyamsoft.tetherfi.server.proxy.SocketTracker
import com.pyamsoft.tetherfi.server.proxy.session.tcp.socks.AbstractSOCKSImplementation
import com.pyamsoft.tetherfi.server.proxy.session.tcp.socks.AbstractSOCKSImplementation.Responder.Companion.INVALID_IP
import com.pyamsoft.tetherfi.server.proxy.session.tcp.socks.AbstractSOCKSImplementation.Responder.Companion.INVALID_PORT
import com.pyamsoft.tetherfi.server.proxy.session.tcp.socks.SOCKSCommand
import com.pyamsoft.tetherfi.server.proxy.session.tcp.socks.five.SOCKS5Implementation.AcceptedAuthenticationMethods.ERROR_NO_ACCEPTABLE_AUTH_METHODS
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.core.writeFully
import io.ktor.utils.io.readByte
import io.ktor.utils.io.readPacket
import io.ktor.utils.io.readShort
import io.ktor.utils.io.writePacket
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import kotlinx.io.Buffer
import kotlinx.io.Sink
import kotlinx.io.bytestring.decodeToString
import kotlinx.io.readByteArray
import kotlinx.io.readByteString

/** https://www.rfc-editor.org/rfc/rfc1928 */
@Singleton
internal class SOCKS5Implementation
@Inject
internal constructor(
    socketTagger: SocketTagger,
) :
    AbstractSOCKSImplementation<SOCKS5AddressType, SOCKS5Implementation.Responder>(
        socketTagger = socketTagger,
    ) {

  @CheckResult
  private suspend fun readDestinationAddress(
      serverDispatcher: ServerDispatcher,
      proxyInput: ByteReadChannel,
      addressType: SOCKS5AddressType
  ): InetAddress =
      withContext(context = serverDispatcher.primary) {
        when (addressType) {
          SOCKS5AddressType.IPV4 -> {
            val data = proxyInput.readPacket(4)
            return@withContext Inet4Address.getByAddress(data.readByteArray())
          }
          SOCKS5AddressType.DOMAIN_NAME -> {
            val addressLength = proxyInput.readByte().toInt()
            val data = proxyInput.readPacket(addressLength)
            return@withContext InetAddress.getByName(data.readByteString().decodeToString())
          }
          SOCKS5AddressType.IPV6 -> {
            val data = proxyInput.readPacket(16)
            return@withContext Inet6Address.getByAddress(data.readByteArray())
          }
        }
      }

  override suspend fun usingResponder(
      proxyOutput: ByteWriteChannel,
      block: suspend Responder.() -> Unit
  ) {
    Responder(proxyOutput).also { block(it) }
  }

  override suspend fun udpAssociate(
      scope: CoroutineScope,
      serverDispatcher: ServerDispatcher,
      socketTracker: SocketTracker,
      connectionInfo: BroadcastNetworkStatus.ConnectionInfo.Connected,
      proxyInput: ByteReadChannel,
      proxyOutput: ByteWriteChannel,
      client: TetherClient,
      destinationAddress: InetAddress,
      destinationPort: Short,
      addressType: SOCKS5AddressType,
      responder: Responder,
      onReport: suspend (ByteTransferReport) -> Unit
  ) {
    Timber.w { "TODO UDP_ASSOC $destinationAddress $destinationPort" }
  }

  override suspend fun handleSocksCommand(
      scope: CoroutineScope,
      serverDispatcher: ServerDispatcher,
      socketTracker: SocketTracker,
      networkBinder: SocketBinder.NetworkBinder,
      proxyInput: ByteReadChannel,
      proxyOutput: ByteWriteChannel,
      connectionInfo: BroadcastNetworkStatus.ConnectionInfo.Connected,
      client: TetherClient,
      onReport: suspend (ByteTransferReport) -> Unit
  ) =
      withContext(context = serverDispatcher.primary) {
        /**
         *      +----------+----------+
         *      | NMETHODS | METHODS  |
         *      +----------+----------+
         *      |    1     | 1 to 255 |
         *      +----------+----------+
         */
        val methodCount = proxyInput.readByte()
        val methods = ByteArray(methodCount.toInt()) { proxyInput.readByte() }

        // Check that we both support the "no-auth" method
        val selectedMethod = AcceptedAuthenticationMethods.METHOD_NO_AUTHENTICATION
        if (!methods.contains(selectedMethod)) {
          usingResponder(proxyOutput) { sendNoValidAuthentication() }
          return@withContext
        }

        /**
         *     +----+--------+
         *     |VER | METHOD |
         *     +----+--------+
         *     | 1  |   1    |
         *     +----+--------+
         */
        // Send the method selection message
        usingResponder(proxyOutput) { sendAuthMethodSelection(selectedMethod) }

        /**
         * +----+-----+-------+------+----------+----------+ |VER | CMD | RSV | ATYP | DST.ADDR |
         * DST.PORT | +----+-----+-------+------+----------+----------+ | 1 | 1 | X'00' | 1 |
         * Variable | 2 | +----+-----+-------+------+----------+----------+
         */
        val versionByte = proxyInput.readByte()
        if (versionByte != SOCKS_VERSION_BYTE) {
          Timber.w { "Invalid SOCKS version byte: $versionByte" }
          usingResponder(proxyOutput) { sendRefusal() }
          return@withContext
        }

        val commandByte = proxyInput.readByte()
        val command = SOCKSCommand.fromByte(commandByte)
        if (command == null) {
          Timber.w {
            "Invalid command byte: $commandByte, expected CONNECT, BIND, or UDP_ASSOCIATE"
          }
          usingResponder(proxyOutput) { sendCommandUnsupported() }
          return@withContext
        }

        val reserved = proxyInput.readByte()
        if (reserved != RESERVED_BYTE) {
          Timber.w { "Expected reserve byte, but got data: $reserved" }
          usingResponder(proxyOutput) { sendRefusal() }
          return@withContext
        }

        val addressTypeByte = proxyInput.readByte()
        val addressType = SOCKS5AddressType.fromAddressType(addressTypeByte)
        if (addressType == null) {
          Timber.w { "Invalid address type: $addressTypeByte" }
          usingResponder(proxyOutput) { sendRefusal() }
          return@withContext
        }

        val destinationAddress =
            try {
              readDestinationAddress(
                  serverDispatcher = serverDispatcher,
                  proxyInput = proxyInput,
                  addressType = addressType,
              )
            } catch (e: Throwable) {
              Timber.e(e) { "Unable to parse the destination address" }
              usingResponder(proxyOutput) { sendError() }
              return@withContext
            }

        val destinationPort = proxyInput.readShort()
        if (destinationPort <= 0) {
          Timber.w { "Invalid destination port $destinationPort" }
          usingResponder(proxyOutput) { sendError() }
          return@withContext
        }

        val responder = Responder(proxyOutput)
        return@withContext performSOCKSCommand(
            addressType = addressType,
            scope = scope,
            socketTracker = socketTracker,
            networkBinder = networkBinder,
            serverDispatcher = serverDispatcher,
            proxyInput = proxyInput,
            proxyOutput = proxyOutput,
            responder = responder,
            client = client,
            destinationAddress = destinationAddress,
            destinationPort = destinationPort,
            command = command,
            connectionInfo = connectionInfo,
            onReport = onReport,
        )
      }

  @JvmInline
  internal value class Responder
  internal constructor(
      private val proxyOutput: ByteWriteChannel,
  ) : AbstractSOCKSImplementation.Responder<SOCKS5AddressType> {

    /**
     *         +----+-----+-------+------+----------+----------+
     *         |VER | REP |  RSV  | ATYP | BND.ADDR | BND.PORT |
     *         +----+-----+-------+------+----------+----------+
     *         | 1  |  1  | X'00' |  1   | Variable |    2     |
     *         +----+-----+-------+------+----------+----------+
     */
    private suspend inline fun sendPacket(builder: Sink.() -> Unit) {
      val packet =
          Buffer().apply {
            // VN
            writeByte(SOCKS_VERSION_BYTE)

            // Builder
            builder()
          }

      proxyOutput.apply {
        writePacket(packet)
        flush()
      }
    }

    private suspend fun sendPacket(
        addressType: SOCKS5AddressType,
        replyCode: Byte,
        port: Short,
        address: InetAddress
    ) {
      sendPacket {
        // CD
        writeByte(replyCode)

        // RSV
        writeByte(RESERVED_BYTE)

        // Address type
        writeByte(addressType.byte)

        // BND.ADDR
        writeFully(address.address)

        // BND.PORT
        writeShort(port)
      }
    }

    override suspend fun sendConnectSuccess(
        addressType: SOCKS5AddressType,
    ) {
      return sendPacket(
          addressType = addressType,
          replyCode = SUCCESS_CODE,
          port = INVALID_PORT,
          address = INVALID_IP,
      )
    }

    override suspend fun sendBindInitialized(
        addressType: SOCKS5AddressType,
        port: Short,
        address: InetAddress,
    ) {
      return sendPacket(
          addressType = addressType,
          replyCode = SUCCESS_CODE,
          port = port,
          address = address,
      )
    }

    override suspend fun sendError() {
      return sendPacket(
          replyCode = ERROR_GENERAL_SERVER_FAIL,

          // These don't matter
          port = INVALID_PORT,
          address = INVALID_IP,
          addressType = SOCKS5AddressType.IPV4,
      )
    }

    override suspend fun sendRefusal() {
      return sendPacket(
          replyCode = ERROR_CONNECTION_REFUSED,
          // These don't matter
          port = INVALID_PORT,
          address = INVALID_IP,
          addressType = SOCKS5AddressType.IPV4,
      )
    }

    suspend fun sendCommandUnsupported() {
      return sendPacket(
          replyCode = ERROR_COMMAND_NOT_SUPPORTED,
          // These don't matter
          port = INVALID_PORT,
          address = INVALID_IP,
          addressType = SOCKS5AddressType.IPV4,
      )
    }

    suspend fun sendNoValidAuthentication() {
      return sendPacket { writeByte(ERROR_NO_ACCEPTABLE_AUTH_METHODS) }
    }

    suspend fun sendAuthMethodSelection(method: Byte) {
      return sendPacket { writeByte(method) }
    }

    companion object {

      // Granted
      private const val SUCCESS_CODE: Byte = 0

      private const val ERROR_GENERAL_SERVER_FAIL: Byte = 1
      private const val ERROR_CONNECTION_REFUSED: Byte = 5
      private const val ERROR_COMMAND_NOT_SUPPORTED: Byte = 7
    }
  }

  /**
   * We do NOT support Password or GSSAPI, so we are NOT a compliant implementation, (and neither
   * are many of the simple projects on GitHub :) )
   *
   * https://www.rfc-editor.org/rfc/rfc1928 page:3
   */
  private object AcceptedAuthenticationMethods {
    const val METHOD_NO_AUTHENTICATION: Byte = 0

    const val ERROR_NO_ACCEPTABLE_AUTH_METHODS = 0xFF.toByte()
  }

  companion object {
    private const val SOCKS_VERSION_BYTE: Byte = 5
    private const val RESERVED_BYTE: Byte = 0
  }
}
