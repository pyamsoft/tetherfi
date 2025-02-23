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

package com.pyamsoft.tetherfi.server.proxy.session.tcp.socks.five

import androidx.annotation.CheckResult
import com.pyamsoft.pydroid.core.cast
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.tetherfi.core.Timber
import com.pyamsoft.tetherfi.server.SocketCreator
import com.pyamsoft.tetherfi.server.clients.TetherClient
import com.pyamsoft.tetherfi.server.proxy.ProxyConnectionInfo
import com.pyamsoft.tetherfi.server.proxy.ServerDispatcher
import com.pyamsoft.tetherfi.server.proxy.SocketTagger
import com.pyamsoft.tetherfi.server.proxy.SocketTracker
import io.ktor.network.sockets.BoundDatagramSocket
import io.ktor.network.sockets.Datagram
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.isClosed
import io.ktor.network.sockets.toJavaAddress
import io.ktor.utils.io.core.build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.io.Buffer
import kotlinx.io.Source
import kotlinx.io.writeUShort

internal class UDPRelayServer(
    private val socketCreator: SocketCreator,
    private val socketTagger: SocketTagger,
    private val socketTracker: SocketTracker,
    private val serverDispatcher: ServerDispatcher,
    private val server: BoundDatagramSocket,
    private val proxyConnectionInfo: ProxyConnectionInfo,
    private val client: TetherClient,
) {

    @CheckResult
    private fun isPacketFromClient(
        address: InetSocketAddress,
        proxyConnectionInfo: ProxyConnectionInfo,
    ): Boolean {
        return address.hostname == proxyConnectionInfo.hostNameOrIp
    }

    @CheckResult
    private suspend fun readInputPacket(packet: Datagram): PacketDestination? {
        val data = packet.packet

        val reservedByteOne = data.readByte()
        if (reservedByteOne != RESERVED_BYTE) {
            Timber.w { "DROP: Expected reserve byte one, but got data: $reservedByteOne" }
            return null
        }

        val reservedByteTwo = data.readByte()
        if (reservedByteTwo != RESERVED_BYTE) {
            Timber.w { "DROP: Expected reserve byte two, but got data: $reservedByteTwo" }
            return null
        }

        val fragment = data.readByte()
        if (fragment != FRAGMENT_ZERO) {
            Timber.w { "DROP: Fragments not supported: $fragment" }
            return null
        }

        return AddressResolver.resolvePacketDestination(
            serverDispatcher = serverDispatcher,
            sourceOrByteReadChannel = SourceOrByteReadChannel.FromSource(
                data
            ),
            onInvalidAddressType = {
                Timber.w { "DROP: Invalid address type in UDP packet: $it" }
            },
            onInvalidDestinationAddress = {
                Timber.w { "DROP: Invalid destination address type in UDP packet" }
            },
        )
    }

    @CheckResult
    private fun buildResponsePacket(data: Source): Source {
        // Build the response message format
        val responseWriter = Buffer()

        // 2 reserved
        responseWriter.writeByte(RESERVED_BYTE)
        responseWriter.writeByte(RESERVED_BYTE)

        // No fragment
        responseWriter.writeByte(FRAGMENT_ZERO)

        // Address type is IPv4
        responseWriter.writeByte(SOCKS5AddressType.IPV4.byte)
        responseWriter.write(
            server.localAddress.toJavaAddress()
                .cast<java.net.InetSocketAddress>().requireNotNull {
                    "server.localAddress was NOT  a java.net.InetSocketAddress"
                }.address
                .requireNotNull { "server.localAddress.address is NULL" }
                .address.requireNotNull {
                    "server.localAddress.address bytes is NULL"
                },
        )
        responseWriter.writeUShort(proxyConnectionInfo.port.toUShort())

        // Data
        responseWriter.transferFrom(data)

        // Respond with our message back to the client
        return responseWriter.build()
    }

    @CheckResult
    fun relay(
        scope: CoroutineScope,
    ): Job = scope.launch(context = serverDispatcher.primary) {
        // We need to create another bound socket that has a "output" port and address to send data
        socketCreator.create { builder ->
            val bound = builder.udp().configure {
                reuseAddress = true

                // As of KTOR-3.0.0, this is not supported and crashes at runtime
                // reusePort = true
            }.also { socketTagger.tagSocket() }
                // TODO patch ktor to allow this to bind to specific network
                .bind()
                .also {
                    // Track server socket
                    socketTracker.track(it)
                }

            bound.use { socket ->
                while (!server.isClosed) {
                    // Wait for a client message
                    val proxyReadPacket = server.receive()
                    val proxyClientAddress =
                        proxyReadPacket.address.cast<InetSocketAddress>().requireNotNull {
                            "proxyReadPacket.address is not InetSocketAddress"
                        }

                    // We expect this to be from the initial connection port
                    if (!isPacketFromClient(proxyClientAddress, proxyConnectionInfo)) {
                        Timber.w { "DROP: Packet was not from expected client: $proxyClientAddress" }
                        return@use
                    }

                    val packetDestination = readInputPacket(proxyReadPacket) ?: return@use

                    // Copy the input data for writing
                    val byteWriter = Buffer().apply {
                        // Read the remaining back into a writer
                        proxyReadPacket.packet.transferTo(this)
                    }
                    // Send the message we got from the client
                    socket.send(
                        Datagram(
                            address = InetSocketAddress(
                                hostname = packetDestination.address.hostAddress.requireNotNull(),
                                port = packetDestination.port.toInt(),
                            ),
                            packet = byteWriter.build()
                        )
                    )

                    // Wait for a UDP response from real upstream
                    val response = socket.receive()

                    // Respond with our message back to the client
                    val responsePacket = buildResponsePacket(response.packet)

                    server.send(
                        Datagram(
                            address = proxyClientAddress,
                            packet = responsePacket,
                        )
                    )
                }
            }
        }
    }

    companion object {
        private const val RESERVED_BYTE: Byte = 0

        // We do NOT support fragments, anything other than the 0 byte should be dropped
        private const val FRAGMENT_ZERO: Byte = 0
    }
}