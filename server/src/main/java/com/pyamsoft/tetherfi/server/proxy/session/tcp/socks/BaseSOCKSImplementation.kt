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
import com.pyamsoft.tetherfi.server.proxy.session.tcp.relayData
import com.pyamsoft.tetherfi.server.proxy.usingConnection
import com.pyamsoft.tetherfi.server.proxy.usingSocketBuilder
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.toJavaAddress
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import java.net.InetAddress
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.withTimeout

internal abstract class BaseSOCKSImplementation<
    AT : BaseSOCKSImplementation.SOCKSAddressType,
    R : BaseSOCKSImplementation.Responder<AT>,
>
protected constructor(
    private val socketTagger: SocketTagger,
) : SOCKSImplementation<R> {

  private suspend fun connect(
      scope: CoroutineScope,
      serverDispatcher: ServerDispatcher,
      socketTracker: SocketTracker,
      networkBinder: SocketBinder.NetworkBinder,
      proxyInput: ByteReadChannel,
      proxyOutput: ByteWriteChannel,
      client: TetherClient,
      destinationAddress: InetAddress,
      destinationPort: Short,
      addressType: AT,
      responder: R,
      onReport: suspend (ByteTransferReport) -> Unit
  ) =
      usingSocketBuilder(dispatcher = serverDispatcher.primary) { builder ->
        val connected =
            try {
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
          val remote = socket.remoteAddress
          Timber.d { "SOCKS CONNECTED: $remote" }
          try {
            // We've successfully connected, tell the client
            responder.sendConnectSuccess(
                addressType = addressType,
                remote = remote.cast<InetSocketAddress>(),
            )
          } catch (e: Throwable) {
            e.ifNotCancellation {
              Timber.e(e) { "Error sending connect() SUCCESS notification" }
              return@usingSocketBuilder
            }
          }

          socket.usingConnection(autoFlush = false) { internetInput, internetOutput ->
            try {
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
            } finally {
              internetOutput.flush()
            }
          }
        }
      }

  private suspend fun bind(
      scope: CoroutineScope,
      serverDispatcher: ServerDispatcher,
      socketTracker: SocketTracker,
      connectionInfo: BroadcastNetworkStatus.ConnectionInfo.Connected,
      proxyInput: ByteReadChannel,
      proxyOutput: ByteWriteChannel,
      client: TetherClient,
      destinationAddress: InetAddress,
      addressType: AT,
      responder: R,
      onReport: suspend (ByteTransferReport) -> Unit
  ) =
      usingSocketBuilder(dispatcher = serverDispatcher.primary) { builder ->
        val bound =
            try {
              builder
                  .tcp()
                  .configure {
                    reuseAddress = true
                    // As of KTOR-3.0.0, this is not supported and crashes at runtime
                    // reusePort = true
                  }
                  .also { socketTagger.tagSocket() }
                  .let { b ->
                    Timber.d { "SOCKS BIND -> ${connectionInfo.hostName}" }
                    b.bind(
                        hostname = connectionInfo.hostName,
                        port = 0,
                        configure = {
                          reuseAddress = true
                          // As of KTOR-3.0.0, this is not supported and crashes at runtime
                          // reusePort = true
                        },
                    )
                  }
                  .use { server ->
                    // Track server socket
                    socketTracker.track(server)

                    // SOCKS protocol says you MUST time out after 2 minutes
                    val boundSocket = scope.async { withTimeout(2.minutes) { server.accept() } }

                    // Once the bind is open, we send the initial reply telling the client
                    // the IP and the port
                    responder.sendBindInitialized(
                        addressType = addressType,
                        bound = server.localAddress.cast(),
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
                  responder.sendError()
                  return@usingSocketBuilder
                }
              }
            }

        // Track this socket for when we fully shut down
        socketTracker.track(bound)

        bound.use { socket ->
          val hostAddress = socket.remoteAddress.cast<InetSocketAddress>().requireNotNull()
          if (hostAddress.toJavaAddress() != destinationAddress) {
            Timber.w { "bind() address $hostAddress != original $destinationAddress" }
            responder.sendRefusal()
            return@usingSocketBuilder
          }

          try {
            responder.sendBindInitialized(
                addressType = addressType,
                bound = hostAddress,
            )
          } catch (e: Throwable) {
            e.ifNotCancellation {
              Timber.e(e) { "Error sending bind() SUCCESS notification" }
              responder.sendError()
              return@usingSocketBuilder
            }
          }

          socket.usingConnection(autoFlush = false) { internetInput, internetOutput ->
            try {
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
            } finally {
              internetOutput.flush()
            }
          }
        }
      }

  protected suspend fun performSOCKSCommand(
      scope: CoroutineScope,
      serverDispatcher: ServerDispatcher,
      socketTracker: SocketTracker,
      connectionInfo: BroadcastNetworkStatus.ConnectionInfo.Connected,
      networkBinder: SocketBinder.NetworkBinder,
      proxyInput: ByteReadChannel,
      proxyOutput: ByteWriteChannel,
      client: TetherClient,
      command: SOCKSCommand,
      destinationPort: Short,
      destinationAddress: InetAddress,
      addressType: AT,
      responder: R,
      onReport: suspend (ByteTransferReport) -> Unit
  ) =
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
              destinationAddress = destinationAddress,
              destinationPort = destinationPort,
              addressType = addressType,
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
              destinationAddress = destinationAddress,
              addressType = addressType,
              onReport = onReport,
          )
        }
        SOCKSCommand.UDP_ASSOCIATE -> {
          udpAssociate(
              scope = scope,
              socketTracker = socketTracker,
              serverDispatcher = serverDispatcher,
              connectionInfo = connectionInfo,
              proxyInput = proxyInput,
              proxyOutput = proxyOutput,
              responder = responder,
              client = client,
              destinationAddress = destinationAddress,
              destinationPort = destinationPort,
              addressType = addressType,
              onReport = onReport,
          )
        }
      }

  protected abstract suspend fun udpAssociate(
      scope: CoroutineScope,
      serverDispatcher: ServerDispatcher,
      socketTracker: SocketTracker,
      connectionInfo: BroadcastNetworkStatus.ConnectionInfo.Connected,
      proxyInput: ByteReadChannel,
      proxyOutput: ByteWriteChannel,
      client: TetherClient,
      destinationAddress: InetAddress,
      destinationPort: Short,
      addressType: AT,
      responder: R,
      onReport: suspend (ByteTransferReport) -> Unit
  )

  internal interface SOCKSAddressType

  internal interface Responder<AT : SOCKSAddressType> : SOCKSImplementation.Responder {

    suspend fun sendRefusal()

    suspend fun sendError()

    suspend fun sendConnectSuccess(
        addressType: AT,
        remote: InetSocketAddress?,
    )

    suspend fun sendBindInitialized(
        addressType: AT,
        bound: InetSocketAddress?,
    )

    companion object {

      internal val DEBUG_SOCKS_REPLIES = false

      /** The zero IP, we send to this IP for error commands */
      internal val INVALID_IPV6_BYTES = ByteArray(16) { 0 }

      /** The zero IP, we send to this IP for error commands */
      internal val INVALID_IPV4_BYTES = ByteArray(4) { 0 }

      /** Zero port sent for error commands */
      internal const val INVALID_PORT: Short = 0

      @CheckResult
      internal fun InetSocketAddress.getJavaInetSocketAddress(): InetAddress {
        return toJavaAddress()
            .cast<java.net.InetSocketAddress>()
            .requireNotNull { "Failed to cast to java.net.InetSocketAddress: $this" }
            .address
            .requireNotNull { "Failed to get IP address from $this" }
      }
    }
  }
}
