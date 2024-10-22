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

import androidx.annotation.CheckResult
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
import com.pyamsoft.tetherfi.server.proxy.session.tcp.TcpSessionTransport
import com.pyamsoft.tetherfi.server.proxy.session.tcp.TransportWriteCommand
import com.pyamsoft.tetherfi.server.proxy.session.tcp.relayData
import com.pyamsoft.tetherfi.server.proxy.usingConnection
import com.pyamsoft.tetherfi.server.proxy.usingSocketBuilder
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.toJavaAddress
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.readByte
import io.ktor.utils.io.readPacket
import io.ktor.utils.io.readShort
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.io.readByteArray
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.minutes

@Singleton
internal class SOCKSTransport @Inject internal constructor(
    private val socketTagger: SocketTagger,
) : TcpSessionTransport<SOCKSProxyRequest> {

    private val dispatcher = Dispatchers.IO

    @CheckResult
    private suspend fun ByteReadChannel.readSOCKSVersion(): SOCKSVersion {
        val versionNumber = this.readByte()
        return SOCKSVersion.fromVersion(versionNumber)
    }

    @CheckResult
    private suspend fun ByteReadChannel.readSOCKSCommand(): SOCKSCommand {
        val command = this.readByte()
        return SOCKSCommand.fromCommand(command)
    }

    @CheckResult
    private suspend fun readUnsignedInt(input: ByteReadChannel): Int {
        val b = input.readByte()
        return java.lang.Byte.toUnsignedInt(b)
    }

    /**
     * We don't support Authentication YET, but we still need to read
     * and then consume the method byte
     */
    @CheckResult
    private suspend fun handleAuthenticationMethod(input: ByteReadChannel): List<Int> {
        val methodCount = readUnsignedInt(input)
        val clientSupportedMethods = List(methodCount) { readUnsignedInt(input) }
        return clientSupportedMethods
    }

    @CheckResult
    private suspend fun ByteReadChannel.readAddress(version: SOCKSVersion): InetAddress {
        val addressType = when (version) {
            SOCKSVersion.SOCKS4 -> SOCKSAddressType.IPV4
            SOCKSVersion.SOCKS5 -> SOCKSAddressType.fromVersion(this.readByte())
        }

        when (addressType) {
            SOCKSAddressType.IPV4 -> {
                val data = readPacket(4)
                return withContext(context = dispatcher) {
                    Inet4Address.getByAddress(data.readByteArray())
                }
            }

            SOCKSAddressType.IPV6 -> {
                val data = readPacket(16)
                return withContext(context = dispatcher) {
                    Inet6Address.getByAddress(data.readByteArray())
                }
            }

            SOCKSAddressType.HOSTNAME -> {
                val size = readUnsignedInt(this)
                val data = readPacket(size)
                return withContext(context = dispatcher) {
                    InetAddress.getByName(data.readByteArray().decodeToString())
                }
            }
        }
    }

    @CheckResult
    private suspend fun receiveRequest(
        version: SOCKSVersion,
        input: ByteReadChannel
    ): SOCKSProxyRequest {
        val command: SOCKSCommand
        val address: InetAddress
        val port: Short
        when (version) {
            SOCKSVersion.SOCKS4 -> {
                command = input.readSOCKSCommand()
                port = input.readShort()
                val ip = input.readAddress(version)

                // Ignore USERID field
                input.readNullTerminatedString()

                address = if (ip.isSOCKS4A()) {
                    withContext(context = dispatcher) {
                        InetAddress.getByName(input.readNullTerminatedString())
                    }
                } else {
                    ip
                }
            }

            SOCKSVersion.SOCKS5 -> {
                command = input.readSOCKSCommand()
                // Reserved field
                input.readByte()
                address = input.readAddress(version)
                port = input.readShort()
            }
        }

        return SOCKSProxyRequest(
            valid = true,
            version = version,
            command = command,
            address = address,
            port = port,
        )
    }

    override suspend fun parseRequest(
        input: ByteReadChannel,
        output: ByteWriteChannel,
    ): SOCKSProxyRequest {
        val version = input.readSOCKSVersion()
        if (version == SOCKSVersion.SOCKS5) {
            val authMethods = handleAuthenticationMethod(input)
            val supportedMethod = authMethods.firstOrNull { it == SOCKS5_AUTH_METHOD_NONE }
            if (supportedMethod == null) {
                Timber.w { "No common auth method found: METHOD_NONE" }
                return SOCKSProxyRequest(
                    valid = false,
                    version = version,
                    command = SOCKSCommand.NONE,
                    address = InetAddress.getLoopbackAddress(),
                    port = 0,
                )
            } else {
                sendAuthenticationReply(output, version, supportedMethod.toByte())
                // TODO support negotiating auth methods
            }
        }

        return receiveRequest(version, input)
    }

    override suspend fun writeProxyOutput(
        output: ByteWriteChannel,
        request: SOCKSProxyRequest,
        command: TransportWriteCommand
    ) {
        val version = request.version
        return when (command) {
            TransportWriteCommand.INVALID -> {
                sendAuthenticationReply(output, version, SOCKS5_NO_ACCEPTABLE_METHODS)
            }

            TransportWriteCommand.BLOCK -> {
                sendFullReply(output, version, version.connectionRefusedCode)
            }

            TransportWriteCommand.ERROR -> {
                sendFullReply(output, version, version.unreachableHostCode)
            }
        }
    }

    private suspend fun connect(
        scope: CoroutineScope,
        serverDispatcher: ServerDispatcher,
        proxyInput: ByteReadChannel,
        proxyOutput: ByteWriteChannel,
        socketTracker: SocketTracker,
        socketTagger: SocketTagger,
        networkBinder: SocketBinder.NetworkBinder,
        client: TetherClient,
        onReport: suspend (ByteTransferReport) -> Unit,
        request: SOCKSProxyRequest,
    ) = usingSocketBuilder(serverDispatcher.primary) { builder ->
        val socket = try {
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
                            InetSocketAddress(request.address.hostName, request.port.toInt())

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
                sendFullReply(
                    proxyOutput, request.version, request.version.unreachableHostCode,
                )
                throw e
            } else {
                e.ifNotCancellation {
                    Timber.e(e) { "Error during socket connect()" }
                    sendFullReply(
                        proxyOutput, request.version, request.version.unreachableHostCode,
                    )
                    throw e
                }
            }
        }

        // Track this socket for when we fully shut down
        socketTracker.track(socket)

        try {
            sendSuccessMessage(
                proxyOutput,
                request.version,
                socket.localAddress,
            )
        } catch (e: Throwable) {
            socket.dispose()
            e.ifNotCancellation {
                Timber.e(e) { "Error sending connect() SUCCESS notification" }
                throw e
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

    private suspend fun bind(
        scope: CoroutineScope,
        connectionInfo: BroadcastNetworkStatus.ConnectionInfo.Connected,
        serverDispatcher: ServerDispatcher,
        proxyInput: ByteReadChannel,
        proxyOutput: ByteWriteChannel,
        socketTracker: SocketTracker,
        socketTagger: SocketTagger,
        client: TetherClient,
        onReport: suspend (ByteTransferReport) -> Unit,
        request: SOCKSProxyRequest,
    ) = usingSocketBuilder(serverDispatcher.primary) { builder ->

        val socket = try {
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

                    sendSuccessMessage(
                        proxyOutput,
                        request.version,
                        server.localAddress,
                    )

                    boundSocket.await()
                }
        } catch (e: Throwable) {
            if (e is TimeoutCancellationException) {
                Timber.w { "Timeout while waiting for socket bind()" }
                sendFullReply(
                    proxyOutput, request.version, request.version.unreachableHostCode,
                )
                throw e
            } else {
                e.ifNotCancellation {
                    Timber.e(e) { "Error during socket bind()" }
                    sendFullReply(
                        proxyOutput, request.version, request.version.unreachableHostCode,
                    )
                    throw e
                }
            }
        }

        // Track this socket for when we fully shut down
        socketTracker.track(socket)

        val hostAddress = socket.remoteAddress.cast<InetSocketAddress>().requireNotNull()
        if (hostAddress.toJavaAddress() != request.address) {
            socket.dispose()
            val msg = "bind() address $hostAddress did not match requested ${request.address}"
            sendFullReply(proxyOutput, request.version, request.version.connectionRefusedCode)
            Timber.w { msg }
            throw SOCKSException(msg)
        }

        try {
            sendSuccessMessage(
                proxyOutput,
                request.version,
                hostAddress,
            )
        } catch (e: Throwable) {
            socket.dispose()
            e.ifNotCancellation {
                Timber.e(e) { "Error sending bind() SUCCESS notification" }
                throw e
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

    private suspend fun udpAssociate(
        scope: CoroutineScope,
        serverDispatcher: ServerDispatcher,
        proxyInput: ByteReadChannel,
        proxyOutput: ByteWriteChannel,
        socketTracker: SocketTracker,
        socketTagger: SocketTagger,
        client: TetherClient,
        onReport: suspend (ByteTransferReport) -> Unit,
        request: SOCKSProxyRequest,
    ) {
        sendFullReply(
            output = proxyOutput, version = request.version,
            SOCKS5_UNSUPPORTED_COMMAND,
        )
        throw SOCKSException("Unsupported UDP_ASSOCIATE command")
    }

    suspend fun handleSOCKSRequest(
        scope: CoroutineScope,
        connectionInfo: BroadcastNetworkStatus.ConnectionInfo.Connected,
        serverDispatcher: ServerDispatcher,
        proxyInput: ByteReadChannel,
        proxyOutput: ByteWriteChannel,
        socketTracker: SocketTracker,
        networkBinder: SocketBinder.NetworkBinder,
        client: TetherClient,
        onReport: suspend (ByteTransferReport) -> Unit,
        request: SOCKSProxyRequest,
    ) = when (request.command) {
        SOCKSCommand.CONNECT -> {
            connect(
                scope = scope,
                serverDispatcher = serverDispatcher,
                proxyInput = proxyInput,
                proxyOutput = proxyOutput,
                socketTagger = socketTagger,
                socketTracker = socketTracker,
                networkBinder = networkBinder,
                request = request,
                client = client,
                onReport = onReport,
            )
        }

        SOCKSCommand.BIND -> {
            bind(
                scope = scope,
                serverDispatcher = serverDispatcher,
                connectionInfo = connectionInfo,
                proxyInput = proxyInput,
                proxyOutput = proxyOutput,
                socketTagger = socketTagger,
                socketTracker = socketTracker,
                request = request,
                client = client,
                onReport = onReport,
            )
        }

        SOCKSCommand.UDP_ASSOCIATE -> {
            udpAssociate(
                scope = scope,
                serverDispatcher = serverDispatcher,
                proxyInput = proxyInput,
                proxyOutput = proxyOutput,
                socketTagger = socketTagger,
                socketTracker = socketTracker,
                request = request,
                client = client,
                onReport = onReport,
            )
        }

        else -> throw SOCKSException("Invalid SOCKS command: $request")
    }

    companion object {

        private const val SOCKS5_AUTH_METHOD_NONE: Int = 0

        private const val SOCKS5_UNSUPPORTED_COMMAND: Byte = 7
        private const val SOCKS5_NO_ACCEPTABLE_METHODS: Byte = 0xFF.toByte()

    }

}