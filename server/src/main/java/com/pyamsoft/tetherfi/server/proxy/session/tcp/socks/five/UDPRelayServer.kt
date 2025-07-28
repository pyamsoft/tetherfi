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
import com.pyamsoft.pydroid.core.ThreadEnforcer
import com.pyamsoft.pydroid.core.cast
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.tetherfi.core.Timber
import com.pyamsoft.tetherfi.server.ServerSocketTimeout
import com.pyamsoft.tetherfi.server.SocketCreator
import com.pyamsoft.tetherfi.server.clients.ByteTransferReport
import com.pyamsoft.tetherfi.server.clients.TetherClient
import com.pyamsoft.tetherfi.server.network.SocketBinder
import com.pyamsoft.tetherfi.server.proxy.ProxyConnectionInfo
import com.pyamsoft.tetherfi.server.proxy.ServerDispatcher
import com.pyamsoft.tetherfi.server.proxy.SocketTagger
import com.pyamsoft.tetherfi.server.proxy.SocketTracker
import com.pyamsoft.tetherfi.server.proxy.session.tcp.enforceBandwidthLimit
import io.ktor.network.sockets.BoundDatagramSocket
import io.ktor.network.sockets.Datagram
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.isClosed
import io.ktor.network.sockets.toJavaAddress
import io.ktor.utils.io.core.build
import java.net.DatagramSocket
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.io.Buffer
import kotlinx.io.InternalIoApi
import kotlinx.io.Source
import kotlinx.io.writeUShort

internal data class UDPRelayServer(
    private val appScope: CoroutineScope,
    private val enforcer: ThreadEnforcer,
    private val socketTagger: SocketTagger,
    private val timeout: ServerSocketTimeout,
    private val networkBinder: SocketBinder.NetworkBinder,
    private val socketCreator: SocketCreator,
    private val socketTracker: SocketTracker,
    private val serverDispatcher: ServerDispatcher,
    private val serverSocket: BoundDatagramSocket,
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

  internal class LastActivityTimeHolder internal constructor() {

    private var lastActivityNano = System.nanoTime()

    // Record our last activity frequently to keep the relay from dying early
    fun recordLastActivity() {
      lastActivityNano = System.nanoTime()
    }

    @CheckResult
    fun asNanoSeconds(): Duration {
      return lastActivityNano.nanoseconds
    }
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
        sourceOrByteReadChannel = SourceOrByteReadChannel.FromSource(data),
        onInvalidAddressType = { Timber.w { "DROP: Invalid address type in UDP packet: $it" } },
        onInvalidDestinationAddress = {
          Timber.w { "DROP: Invalid destination address type in UDP packet" }
        },
    )
  }

  @CheckResult
  private fun buildResponsePacket(data: Source): Source {
    return Buffer()
        .apply {
          // 2 reserved
          writeByte(RESERVED_BYTE)
          writeByte(RESERVED_BYTE)

          // No fragment
          writeByte(FRAGMENT_ZERO)

          // Address type is IPv4
          writeByte(SOCKS5AddressType.IPV4.byte)
          write(
              serverSocket.localAddress
                  .toJavaAddress()
                  .cast<java.net.InetSocketAddress>()
                  .requireNotNull { "server.localAddress was NOT a java.net.InetSocketAddress" }
                  .address
                  .requireNotNull { "server.localAddress.address is NULL" }
                  .address
                  .requireNotNull { "server.localAddress.address.address is NULL" },
          )
          writeUShort(proxyConnectionInfo.port.toUShort())

          // Data
          transferFrom(data)
        }
        .build()
  }

  @CheckResult
  @OptIn(InternalIoApi::class)
  inline fun relay(
      scope: CoroutineScope,
      crossinline onError: suspend (Throwable) -> Unit,
      crossinline onReport: suspend (ByteTransferReport) -> Unit,
  ): Job =
      scope.launch(context = serverDispatcher.primary) {
        // We need to create another bound socket that has a "output" port and address to send data
        socketCreator.create(
            type = SocketCreator.Type.SERVER,
            onError = {
              // This error comes from the SelectorManager launch {} scope,
              // so everything may be dead. fallback to Dispatchers.IO since we cannot be guaranteed
              // that
              // our custom dispatcher pool is around
              appScope.launch(context = Dispatchers.IO) { onError(it) }
            },
            onBuild = { builder ->
              val bound =
                  builder
                      .udp()
                      .configure {
                        reuseAddress = true

                        // As of KTOR-3.0.0, this is not supported and crashes at runtime
                        // reusePort = true
                      }
                      .also { socketTagger.tagSocket() }
                      .bindWithConfiguration(
                          onBeforeBind = { maybeDatagramSocket ->
                            // This is an Any type because ktor itself does not rely on any Java
                            //
                            // so we have to pass an Any and then cast it out here in a context
                            // where
                            // we DO understand java
                            val datagramSocket = maybeDatagramSocket.cast<DatagramSocket>()
                            if (datagramSocket != null) {
                              networkBinder.bindToNetwork(datagramSocket)
                            }
                          })
                      .also {
                        // Track server socket
                        socketTracker.track(it)
                      }

              // Keep track of the most recent "activity" time
              val lastActivityTime = LastActivityTimeHolder()

              // Watch the client for timeout
              //
              // IF the last message we saw/sent was outside of the timeout period,
              // kill this loop
              val timeoutAfter = timeout.timeoutDuration
              if (!timeoutAfter.isInfinite()) {
                Timber.d { "Watch for UDP relay timeout $timeoutAfter" }
                scope.launch(context = serverDispatcher.sideEffect) {
                  while (!serverSocket.isClosed && isActive) {
                    delay(timeoutAfter)

                    val nowNano = System.nanoTime()
                    val timeDiff = nowNano.nanoseconds - lastActivityTime.asNanoSeconds()
                    if (timeDiff > timeoutAfter) {
                      Timber.w { "UDP relay has gone too long without activity. Close $client" }
                      serverSocket.close()
                    }
                  }
                }
              }

              // Proxy Reporting
              val proxyToInternetBytes = MutableStateFlow(0L)
              val internetToProxyBytes = MutableStateFlow(0L)

              val sendReport = suspend {
                val report =
                    ByteTransferReport(
                        // Reset back to 0 on send
                        // If you don't reset, we will keep on sending a higher and higher number
                        internetToProxy = internetToProxyBytes.getAndUpdate { 0 },
                        proxyToInternet = proxyToInternetBytes.getAndUpdate { 0 },
                    )
                onReport(report)
              }

              // Periodically report the transfer status
              val reportJob =
                  scope.launch(context = serverDispatcher.sideEffect) {
                    while (!serverSocket.isClosed && isActive) {
                      delay(5.seconds)
                      sendReport()
                    }
                  }

              // Rate Limiting (inline for performance)
              val bandwidthLimit = client.bandwidthLimit?.bytes ?: 0L
              val mustEnforceBandwidthLimit = bandwidthLimit > 0
              var startTimeNanos = System.nanoTime()

              try {
                bound.use { socket ->
                  while (!serverSocket.isClosed && isActive) {
                    // Wait for a client message
                    val proxyReadPacket = serverSocket.receive()

                    lastActivityTime.recordLastActivity()

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
                    val byteWriter =
                        Buffer()
                            .apply {
                              // Read the remaining back into a writer
                              proxyReadPacket.packet.transferTo(this)
                            }
                            .build()

                    // Grab the amount we are going to write
                    val writeAmount = byteWriter.buffer.size

                    // Send the message we got from the client
                    socket.send(
                        Datagram(
                            address =
                                InetSocketAddress(
                                    hostname =
                                        packetDestination.address.hostAddress.requireNotNull(),
                                    port = packetDestination.port.toInt(),
                                ),
                            packet = byteWriter,
                        ),
                    )

                    // Record the write
                    if (writeAmount > 0) {
                      proxyToInternetBytes.update { it + writeAmount }
                    }

                    lastActivityTime.recordLastActivity()

                    // Wait for a UDP response from real upstream
                    val response = socket.receive()

                    lastActivityTime.recordLastActivity()

                    // Respond with our message back to the client
                    val responsePacket = buildResponsePacket(response.packet)

                    // Grab the size we have read
                    val readAmount = responsePacket.buffer.size

                    serverSocket.send(
                        Datagram(
                            address = proxyClientAddress,
                            packet = responsePacket,
                        ),
                    )

                    // Record the read
                    if (readAmount > 0) {
                      internetToProxyBytes.update { it + readAmount }
                    }

                    lastActivityTime.recordLastActivity()

                    // Rate Limiting
                    if (mustEnforceBandwidthLimit) {
                      val resetNewTimeNanos =
                          enforceBandwidthLimit(
                              client = client,
                              bandwidthLimit = bandwidthLimit,
                              startTimeNanos = startTimeNanos,
                              read = readAmount,
                          )
                      if (resetNewTimeNanos > 0) {
                        startTimeNanos = resetNewTimeNanos
                      }

                      // Record activity again in case we were delayed by bandwidth limiter
                      lastActivityTime.recordLastActivity()
                    }
                  }
                }
              } finally {
                reportJob.cancel()
                sendReport()
              }
            },
        )
      }

  companion object {
    private const val RESERVED_BYTE: Byte = 0

    // We do NOT support fragments, anything other than the 0 byte should be dropped
    private const val FRAGMENT_ZERO: Byte = 0
  }
}
