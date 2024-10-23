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
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.pydroid.util.ifNotCancellation
import com.pyamsoft.tetherfi.core.Timber
import com.pyamsoft.tetherfi.server.broadcast.BroadcastNetworkStatus
import com.pyamsoft.tetherfi.server.clients.ByteTransferReport
import com.pyamsoft.tetherfi.server.clients.TetherClient
import com.pyamsoft.tetherfi.server.network.SocketBinder
import com.pyamsoft.tetherfi.server.proxy.ServerDispatcher
import com.pyamsoft.tetherfi.server.proxy.SocketTagger
import com.pyamsoft.tetherfi.server.proxy.SocketTracker
import com.pyamsoft.tetherfi.server.proxy.session.tcp.relayData
import com.pyamsoft.tetherfi.server.proxy.session.tcp.socks.SOCKSCommand
import com.pyamsoft.tetherfi.server.proxy.session.tcp.socks.SOCKSImplementation
import com.pyamsoft.tetherfi.server.proxy.session.tcp.socks.readUntilNullTerminator
import com.pyamsoft.tetherfi.server.proxy.usingConnection
import com.pyamsoft.tetherfi.server.proxy.usingSocketBuilder
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.toJavaAddress
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.core.use
import io.ktor.utils.io.core.writeFully
import io.ktor.utils.io.readByte
import io.ktor.utils.io.readPacket
import io.ktor.utils.io.readShort
import io.ktor.utils.io.writePacket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import java.net.Inet4Address
import java.net.InetAddress
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.minutes

/**
 * https://www.openssh.com/txt/socks4.protocol
 */
@Singleton
internal class SOCKS4Implementation @Inject internal constructor(
    private val socketTagger: SocketTagger,
) : SOCKSImplementation<SOCKS4Implementation.Responder> {

    @SuppressLint("CheckResult")
    private suspend fun ignoreUserId(proxyInput: ByteReadChannel) {
        proxyInput.readUntilNullTerminator()
    }

    private suspend fun bind(
        scope: CoroutineScope,
        serverDispatcher: ServerDispatcher,
        socketTracker: SocketTracker,
        connectionInfo: BroadcastNetworkStatus.ConnectionInfo.Connected,
        responder: Responder,
        proxyInput: ByteReadChannel,
        proxyOutput: ByteWriteChannel,
        client: TetherClient,
        originalDestinationAddress: InetAddress,
        onReport: suspend (ByteTransferReport) -> Unit
    ) = usingSocketBuilder(dispatcher = serverDispatcher.primary) { builder ->
        val bound = try {
            builder
                .tcp()
                .configure {
                    reuseAddress = true
                    // As of KTOR-3.0.0, this is not supported and crashes at runtime
                    // reusePort = true
                }
                .also { socketTagger.tagSocket() }
                .let { b ->
                    b.bind(
                        hostname = connectionInfo.hostName,
                        port = 0,
                        configure = {
                            reuseAddress = true
                            // As of KTOR-3.0.0, this is not supported and crashes at runtime
                            // reusePort = true
                        }
                    )
                }
                .use { server ->
                    // SOCKS protocol says you MUST time out after 2 minutes
                    val boundSocket = scope.async { withTimeout(2.minutes) { server.accept() } }

                    // Once the bind is open, we send the initial reply telling the client
                    // the IP and the port
                    val bindAddress = server.localAddress.cast<InetSocketAddress>().requireNotNull()
                    responder.sendBindInitialized(
                        port = bindAddress.port.toShort(),
                        address = bindAddress.cast<java.net.InetSocketAddress>()
                            .requireNotNull().address,
                    )

                    boundSocket.await()
                }
        } catch (e: Throwable) {
            if (e is TimeoutCancellationException) {
                Timber.w { "Timeout while waiting for socket bind()" }
                responder.sendRefusal()

                // Rethrow a cancellation exception
                throw e
            } else {
                e.ifNotCancellation {
                    Timber.e(e) { "Error during socket bind()" }
                    responder.sendRefusal()
                    return@usingSocketBuilder
                }
            }
        }

        // Track this socket for when we fully shut down
        socketTracker.track(bound)

        bound.use { socket ->
            val hostAddress = socket.remoteAddress.cast<InetSocketAddress>().requireNotNull()
            if (hostAddress.toJavaAddress() != originalDestinationAddress) {
                responder.sendRefusal()
                Timber.w { "bind() address $hostAddress != original $originalDestinationAddress" }
                return@usingSocketBuilder
            }

            try {
                responder.sendBindInitialized(
                    port = hostAddress.port.toShort(),
                    address = hostAddress.cast<java.net.InetSocketAddress>()
                        .requireNotNull().address,
                )
            } catch (e: Throwable) {
                e.ifNotCancellation {
                    Timber.e(e) { "Error sending bind() SUCCESS notification" }
                    return@usingSocketBuilder
                }
            }

            socket.usingConnection(autoFlush = false) { internetInput, internetOutput ->
                relayData(
                    scope = scope,
                    client = client,
                    serverDispatcher = serverDispatcher,
                    proxyInput = proxyInput,
                    proxyOutput = proxyOutput,
                    internetInput = internetInput,
                    internetOutput = internetOutput,
                    onReport = onReport,
                )
            }
        }
    }

    private suspend fun connect(
        scope: CoroutineScope,
        responder: Responder,
        serverDispatcher: ServerDispatcher,
        socketTracker: SocketTracker,
        networkBinder: SocketBinder.NetworkBinder,
        proxyInput: ByteReadChannel,
        proxyOutput: ByteWriteChannel,
        client: TetherClient,
        destinationAddress: InetAddress,
        destinationPort: Short,
        onReport: suspend (ByteTransferReport) -> Unit
    ) = usingSocketBuilder(dispatcher = serverDispatcher.primary) { builder ->
        val connected = try {
            builder
                .tcp()
                .configure {
                    reuseAddress = true
                    // As of KTOR-3.0.0, this is not supported and crashes at runtime
                    // reusePort = true
                }
                .also { socketTagger.tagSocket() }
                .let { b ->
                    // SOCKS protocol says you MUST time out after 2 minutes
                    withTimeout(2.minutes) {
                        val remote =
                            InetSocketAddress(
                                hostname = destinationAddress.hostName,
                                port = destinationPort.toInt(),
                            )

                        Timber.d { "SOCKS CONNECT => $remote" }

                        // This function uses our custom build of KTOR
                        // which adds the [onBeforeConnect] hook to allow us
                        // to use the socket created BEFORE connection starts.
                        b.connectWithConfiguration(
                            remoteAddress = remote,
                            configure = {
                                // By default KTOR does not close sockets until "infinity" is reached.
                                socketTimeout = 1.minutes.inWholeMilliseconds
                            },
                            onBeforeConnect = { networkBinder.bindToNetwork(it) },
                        )
                    }
                }
        } catch (e: Throwable) {
            if (e is TimeoutCancellationException) {
                Timber.w { "Timeout while waiting for socket connect()" }
                responder.sendRefusal()

                // Re-throw cancellation exceptions
                throw e
            } else {
                e.ifNotCancellation {
                    Timber.e(e) { "Error during socket connect()" }
                    responder.sendRefusal()
                    return@usingSocketBuilder
                }
            }
        }

        // Track this socket for when we fully shut down
        socketTracker.track(connected)

        connected.use { socket ->
            try {
                // We've successfully connected, tell the client
                responder.sendConnectSuccess()
            } catch (e: Throwable) {
                e.ifNotCancellation {
                    Timber.e(e) { "Error sending connect() SUCCESS notification" }
                    return@usingSocketBuilder
                }
            }

            socket.usingConnection(autoFlush = false) { internetInput, internetOutput ->
                relayData(
                    scope = scope,
                    client = client,
                    proxyInput = proxyInput,
                    proxyOutput = proxyOutput,
                    internetInput = internetInput,
                    internetOutput = internetOutput,
                    serverDispatcher = serverDispatcher,
                    onReport = onReport,
                )
            }
        }
    }

    override suspend fun usingResponder(
        proxyOutput: ByteWriteChannel,
        block: suspend Responder.() -> Unit
    ) {
        Responder(proxyOutput).also { block(it) }
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
    ) = withContext(context = serverDispatcher.primary) {
        /**
         * 		         +----+----+----+----+----+----+----+----+----+....+----+
         * 		         | CD | DSTPORT |      DSTIP        | USERID       |NULL|
         * 		         +----+----+----+----+----+----+----+----+----+....+----+
         *  # of bytes:	   1      2              4           variable       1
         */
        // We've already consumed the first byte of this and determined it was a SOCKS4 request

        // First byte command
        val commandByte = proxyInput.readByte()

        // 2 bytes for port
        val destinationPort = proxyInput.readShort()

        if (destinationPort <= 0) {
            Timber.w { "Invalid destination port $destinationPort" }
            usingResponder(proxyOutput) { sendRefusal() }
            return@withContext
        }

        // 4 bytes IP
        val destinationPacket = proxyInput.readPacket(4)
        val destinationIPBytes = destinationPacket.readByteArray()
        val versionLessDestinationAddress = try {
            InetAddress.getByAddress(destinationIPBytes)
        } catch (e: UnknownHostException) {
            Timber.e(e) { "Unable to parse IPv4 address $destinationIPBytes" }
            usingResponder(proxyOutput) { sendRefusal() }
            return@withContext
        }

        // If this is null, its not an IPv4 address
        val ipv4DestinationAddress = versionLessDestinationAddress.cast<Inet4Address>()
        if (ipv4DestinationAddress == null) {
            Timber.w { "Destination address is not an IPv4 address: $versionLessDestinationAddress" }
            usingResponder(proxyOutput) { sendRefusal() }
            return@withContext
        }

        // Ignore the user ID, we don't implement this
        // And strip off the final NULL byte
        ignoreUserId(proxyInput)

        // If this is a SOCKS4 address (3 zero bytes, read AGAIN and this up until a null byte is the hostname
        val finalDestinationAddress: InetAddress
        if (ipv4DestinationAddress.isSOCKS4A()) {
            val hostname = proxyInput.readUntilNullTerminator()
            val versionLessHostname = try {
                InetAddress.getByName(hostname)
            } catch (e: UnknownHostException) {
                Timber.e(e) { "Unable to parse SOCKS4A IPv4 hostname $hostname" }
                usingResponder(proxyOutput) { sendRefusal() }
                return@withContext
            }
            finalDestinationAddress = versionLessHostname
        } else {
            finalDestinationAddress = ipv4DestinationAddress
        }

        // Now we've read everything. Check the command
        val command = SOCKSCommand.fromByte(commandByte)
        if (command == null) {
            Timber.w { "Invalid command byte: $commandByte, expected CONNECT, BIND, or UDP_ASSOCIATE" }
            usingResponder(proxyOutput) { sendRefusal() }
            return@withContext
        }

        if (command == SOCKSCommand.UDP_ASSOCIATE) {
            Timber.w { "SOCKS4 implementation does not support UDP_ASSOCIATE" }
            usingResponder(proxyOutput) { sendRefusal() }
            return@withContext
        } else {
            val responder = Responder(proxyOutput)
            when (command) {
                SOCKSCommand.CONNECT -> {
                    connect(
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
                        onReport = onReport,
                    )
                }

                SOCKSCommand.BIND -> {
                    bind(
                        scope = scope,
                        socketTracker = socketTracker,
                        serverDispatcher = serverDispatcher,
                        connectionInfo = connectionInfo,
                        proxyInput = proxyInput,
                        proxyOutput = proxyOutput,
                        responder = responder,
                        client = client,
                        originalDestinationAddress = finalDestinationAddress,
                        onReport = onReport,
                    )
                }

                else -> {
                    throw IllegalStateException("SOCKS4 command was $command but we expected BIND or CONNECT")
                }
            }
        }


    }

    @ConsistentCopyVisibility
    internal data class Responder internal constructor(
        val proxyOutput: ByteWriteChannel,
    ) : SOCKSImplementation.Responder {

        /**
         * 		         +----+----+----+----+----+----+----+----+
         * 		         | VN | CD | DSTPORT |      DSTIP        |
         * 		         +----+----+----+----+----+----+----+----+
         *  # of bytes:	   1    1      2              4
         */
        private suspend fun sendPacket(
            replyCode: Byte,
            port: Short,
            address: InetAddress
        ) {
            val packet = Buffer().apply {
                // VN
                writeByte(0)

                // CD
                writeByte(replyCode)

                // DSTPORT
                writeShort(port)

                // DSTIP
                writeFully(
                    address.address, offset = 0, length = 4,
                )
            }

            proxyOutput.apply {
                writePacket(packet)
                flush()
            }
        }

        suspend fun sendConnectSuccess() {
            return sendPacket(
                replyCode = SUCCESS_CODE,
                port = INVALID_PORT,
                address = INVALID_IP,
            )
        }

        suspend fun sendBindInitialized(
            port: Short,
            address: InetAddress,
        ) {
            return sendPacket(
                replyCode = SUCCESS_CODE,
                port = port,
                address = address,
            )
        }

        suspend fun sendRefusal() {
            return sendPacket(
                replyCode = ERROR_CODE,
                port = INVALID_PORT,
                address = INVALID_IP,
            )
        }

        companion object {

            // Granted
            private const val SUCCESS_CODE: Byte = 90

            // SOCKS4 does not differentiate between "something went wrong" and "no"
            private const val ERROR_CODE: Byte = 91

            /**
             * The zero IP, we send to this IP for error commands
             */
            private val INVALID_IP = InetAddress.getByAddress(byteArrayOf(0, 0, 0, 0))

            /**
             * Zero port sent for error commands
             */
            private const val INVALID_PORT: Short = 0

        }

    }
}