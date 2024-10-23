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

package com.pyamsoft.tetherfi.server.proxy.session.tcp.socks.four

import android.annotation.SuppressLint
import com.pyamsoft.pydroid.core.cast
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
import com.pyamsoft.tetherfi.server.proxy.session.tcp.socks.readUntilNullTerminator
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.core.writeFully
import io.ktor.utils.io.readByte
import io.ktor.utils.io.readPacket
import io.ktor.utils.io.readShort
import io.ktor.utils.io.writePacket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import kotlinx.io.Buffer
import kotlinx.io.Sink
import kotlinx.io.readByteArray

/** https://www.openssh.com/txt/socks4.protocol */
@Singleton
internal class SOCKS4Implementation
@Inject
internal constructor(
    socketTagger: SocketTagger,
) :
    AbstractSOCKSImplementation<SOCKS4AddressType, SOCKS4Implementation.Responder>(
        socketTagger = socketTagger) {

  @SuppressLint("CheckResult")
  private suspend fun ignoreUserId(proxyInput: ByteReadChannel) {
    proxyInput.readUntilNullTerminator()
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
      addressType: SOCKS4AddressType,
      responder: Responder,
      onReport: suspend (ByteTransferReport) -> Unit
  ) {
    Timber.w { "SOCKS4 implementation does not support UDP_ASSOCIATE" }
    usingResponder(proxyOutput) { sendRefusal() }
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
         * +----+----+----+----+----+----+----+----+----+....+----+ | CD | DSTPORT | DSTIP | USERID
         * |NULL| +----+----+----+----+----+----+----+----+----+....+----+
         *
         * # of bytes:	 1 2 4 variable 1
         */
        // We've already consumed the first byte of this and determined it was a SOCKS4 request

        // First byte command
        val commandByte = proxyInput.readByte()
        val command = SOCKSCommand.fromByte(commandByte)
        if (command == null) {
          Timber.w {
            "Invalid command byte: $commandByte, expected CONNECT, BIND, or UDP_ASSOCIATE"
          }
          usingResponder(proxyOutput) { sendRefusal() }
          return@withContext
        }

        // 2 bytes for port
        val destinationPort = proxyInput.readShort()

        if (destinationPort <= 0) {
          Timber.w { "Invalid destination port $destinationPort" }
          usingResponder(proxyOutput) { sendError() }
          return@withContext
        }

        // 4 bytes IP
        val destinationPacket = proxyInput.readPacket(4)
        val destinationIPBytes = destinationPacket.readByteArray()
        val versionLessDestinationAddress =
            try {
              InetAddress.getByAddress(destinationIPBytes)
            } catch (e: UnknownHostException) {
              Timber.e(e) { "Unable to parse IPv4 address $destinationIPBytes" }
              usingResponder(proxyOutput) { sendError() }
              return@withContext
            }

        // If this is null, its not an IPv4 address
        val ipv4DestinationAddress = versionLessDestinationAddress.cast<Inet4Address>()
        if (ipv4DestinationAddress == null) {
          Timber.w { "Destination address is not an IPv4 address: $versionLessDestinationAddress" }
          usingResponder(proxyOutput) { sendError() }
          return@withContext
        }

        // Ignore the user ID, we don't implement this
        // And strip off the final NULL byte
        ignoreUserId(proxyInput)

        // If this is a SOCKS4 address (3 zero bytes, read AGAIN and this up until a null byte is
        // the hostname
        val finalDestinationAddress: InetAddress
        if (ipv4DestinationAddress.isSOCKS4A()) {
          val hostname = proxyInput.readUntilNullTerminator()
          val versionLessHostname =
              try {
                InetAddress.getByName(hostname)
              } catch (e: UnknownHostException) {
                Timber.e(e) { "Unable to parse SOCKS4A IPv4 hostname $hostname" }
                usingResponder(proxyOutput) { sendError() }
                return@withContext
              }
          finalDestinationAddress = versionLessHostname
        } else {
          finalDestinationAddress = ipv4DestinationAddress
        }

        val responder = Responder(proxyOutput)
        return@withContext performSOCKSCommand(
            addressType = SOCKS4AddressType,
            scope = scope,
            socketTracker = socketTracker,
            networkBinder = networkBinder,
            serverDispatcher = serverDispatcher,
            proxyInput = proxyInput,
            proxyOutput = proxyOutput,
            responder = responder,
            client = client,
            destinationAddress = finalDestinationAddress,
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
  ) : AbstractSOCKSImplementation.Responder<SOCKS4AddressType> {

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

    /**
     * +----+----+----+----+----+----+----+----+ | VN | CD | DSTPORT | DSTIP |
     * +----+----+----+----+----+----+----+----+
     *
     * # of bytes:	 1 1 2 4
     */
    private suspend fun sendPacket(replyCode: Byte, port: Short, address: InetAddress) {
      sendPacket {
        // CD
        writeByte(replyCode)

        // DSTPORT
        writeShort(port)

        // DSTIP
        writeFully(address.address)
      }
    }

    override suspend fun sendConnectSuccess(addressType: SOCKS4AddressType) {
      return sendPacket(
          replyCode = SUCCESS_CODE,
          port = INVALID_PORT,
          address = INVALID_IP,
      )
    }

    override suspend fun sendBindInitialized(
        addressType: SOCKS4AddressType,
        port: Short,
        address: InetAddress
    ) {
      return sendPacket(
          replyCode = SUCCESS_CODE,
          port = port,
          address = address,
      )
    }

    override suspend fun sendRefusal() {
      return sendPacket(
          replyCode = ERROR_CODE,
          port = INVALID_PORT,
          address = INVALID_IP,
      )
    }

    override suspend fun sendError() {
      // SOCKS4 does not differentiate between "something went wrong" and "no"
      return sendRefusal()
    }

    companion object {

      private const val SOCKS_VERSION_BYTE: Byte = 0

      // Granted
      private const val SUCCESS_CODE: Byte = 90

      // SOCKS4 does not differentiate between "something went wrong" and "no"
      private const val ERROR_CODE: Byte = 91
    }
  }
}
