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
import com.pyamsoft.tetherfi.server.proxy.ProxyConnectionInfo
import com.pyamsoft.tetherfi.server.proxy.ServerDispatcher
import io.ktor.network.sockets.BoundDatagramSocket
import io.ktor.network.sockets.Datagram
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.isClosed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal class UDPRelayServer(
    private val serverDispatcher: ServerDispatcher,
    private val server: BoundDatagramSocket,
    private val proxyConnectionInfo: ProxyConnectionInfo,
) {

    @CheckResult
    private fun isPacketFromClient(
        packet: Datagram,
        proxyConnectionInfo: ProxyConnectionInfo,
    ): Boolean {
        val inet = packet.address.cast<InetSocketAddress>().requireNotNull()
        return inet.hostname == proxyConnectionInfo.hostNameOrIp
    }

    @CheckResult
    fun relay(
        scope: CoroutineScope,
    ): Job = scope.launch(context = serverDispatcher.primary) {
        while (!server.isClosed) {
            val readPacket = server.receive()
            val writePacket =
                if (isPacketFromClient(readPacket, proxyConnectionInfo)) {
                    val inputData = readPacket.packet
                    val reservedByteOne = inputData.readByte()
                    if (reservedByteOne != RESERVED_BYTE) {
                        Timber.w { "DROP: Expected reserve byte one, but got data: $reservedByteOne" }
                        return@launch
                    }

                    val reservedByteTwo = inputData.readByte()
                    if (reservedByteTwo != RESERVED_BYTE) {
                        Timber.w { "DROP: Expected reserve byte two, but got data: $reservedByteTwo" }
                        return@launch
                    }

                    val fragment = inputData.readByte()
                    if (fragment != FRAGMENT_ZERO) {
                        Timber.w { "DROP: Fragments not supported: $fragment" }
                        return@launch
                    }

                    val packetDestination = AddressResolver.resolvePacketDestination(
                        serverDispatcher = serverDispatcher,
                        sourceOrByteReadChannel = SourceOrByteReadChannel.FromSource(
                            inputData
                        ),
                        onInvalidAddressType = {
                            Timber.w { "DROP: Invalid address type in UDP packet: $it" }
                        },
                        onInvalidDestinationAddress = {
                            Timber.w { "DROP: Invalid destination address type in UDP packet" }
                        },
                    )

                    if (packetDestination == null) {
                        return@launch
                    }

                    // Send packet data to destination address
                    Datagram(
                        address =
                        InetSocketAddress(
                            hostname = packetDestination.address.hostAddress.requireNotNull(),
                            port = packetDestination.port.toInt(),
                        ),
                        packet = inputData
                    )
                        .also {
                            Timber.d { "SEND TO DEST: $packetDestination -> $it" }
                        }
                } else {
                    Datagram(
                        address = proxyConnectionInfo.address,
                        packet = readPacket.packet
                    )
                        .also {
                            Timber.d {
                                "BACK TO PROXY: server=$proxyConnectionInfo -> $it"
                            }
                        }
                }
            server.send(writePacket)
        }
    }

    companion object {
        private const val RESERVED_BYTE: Byte = 0

        // We do NOT support fragments, anything other than the 0 byte should be dropped
        private const val FRAGMENT_ZERO: Byte = 0
    }
}