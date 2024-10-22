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

import com.pyamsoft.pydroid.core.cast
import com.pyamsoft.pydroid.core.requireNotNull
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.SocketAddress
import io.ktor.network.sockets.toJavaAddress
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.core.build
import io.ktor.utils.io.core.writeFully
import io.ktor.utils.io.writePacket
import kotlinx.io.Buffer
import kotlinx.io.Sink
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress

private const val SOCKS5_RESERVED: Byte = 0
private val EMPTY_INET_ADDRESS = InetSocketAddress(hostname = "0.0.0.0", 0)

private val EMPTY_AND_THEN: Sink.() -> Unit = {}

private suspend inline fun sendReply(
    output: ByteWriteChannel,
    version: SOCKSVersion,
    value: Byte,
    andThen: Sink.() -> Unit,
) {
    val packet = Buffer().apply {
        writeByte(version.replyVersion)
        writeByte(value)
        andThen()
    }.build()
    output.apply {
        writePacket(packet)
        flush()
    }
}

private suspend fun sendReplyToAddress(
    output: ByteWriteChannel,
    version: SOCKSVersion,
    value: Byte,
    address: InetSocketAddress
) {
    sendReply(output = output, version = version, value = value) {
        if (version == SOCKSVersion.SOCKS5) {
            writeByte(SOCKS5_RESERVED)
        }
        writeAddress(version, address)
    }
}

internal suspend inline fun sendAuthenticationReply(
    output: ByteWriteChannel,
    version: SOCKSVersion,
    value: Byte,
) {
    sendReply(
        output = output,
        version = version,
        value = value,
        andThen = EMPTY_AND_THEN,
    )
}

internal suspend fun sendFullReply(
    output: ByteWriteChannel,
    version: SOCKSVersion,
    value: Byte,
) {
    sendReplyToAddress(
        output = output, version = version, value = value,
        address = EMPTY_INET_ADDRESS,
    )
}

internal suspend fun sendSuccessMessage(
    output: ByteWriteChannel,
    version: SOCKSVersion,
    address: SocketAddress
) {
    sendReplyToAddress(
        output = output,
        version = version,
        value = version.successCode,
        address = address as InetSocketAddress,
    )
}

private fun Sink.writeAddress(
    version: SOCKSVersion,
    address: InetSocketAddress,
) {
    val port = address.port.toShort()
    val ip = address.toJavaAddress().cast<InetAddress>().requireNotNull()
    when (version) {
        SOCKSVersion.SOCKS4 -> {
            writeShort(port)
            writeFully(ip.address, length = 4)
        }

        SOCKSVersion.SOCKS5 -> {
            when (ip) {
                is Inet4Address -> writeByte(SOCKSAddressType.IPV4.version)
                is Inet6Address -> writeByte(SOCKSAddressType.IPV6.version)
                else -> error("Unknown InetAddress type: $ip")
            }
            writeFully(ip.address)
            writeShort(port)
        }
    }
}
