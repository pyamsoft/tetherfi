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

package com.pyamsoft.tetherfi.server.proxy.manager

import androidx.annotation.CheckResult
import com.pyamsoft.pydroid.bus.EventConsumer
import com.pyamsoft.pydroid.core.ThreadEnforcer
import com.pyamsoft.pydroid.util.ifNotCancellation
import com.pyamsoft.tetherfi.core.AppDevEnvironment
import com.pyamsoft.tetherfi.server.broadcast.BroadcastNetworkStatus
import com.pyamsoft.tetherfi.server.event.ServerStopRequestEvent
import com.pyamsoft.tetherfi.server.network.SocketBinder
import com.pyamsoft.tetherfi.server.proxy.ServerDispatcher
import com.pyamsoft.tetherfi.server.proxy.SharedProxy
import com.pyamsoft.tetherfi.server.proxy.SocketTagger
import com.pyamsoft.tetherfi.server.proxy.SocketTracker
import com.pyamsoft.tetherfi.server.proxy.session.ProxySession
import com.pyamsoft.tetherfi.server.proxy.session.tcp.TcpProxyData
import com.pyamsoft.tetherfi.server.proxy.usingConnection
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.ServerSocket
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.SocketBuilder
import io.ktor.network.sockets.SocketTimeoutException
import io.ktor.network.sockets.isClosed
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.time.Duration

internal class TcpProxyManager
internal constructor(
    private val socketBinder: SocketBinder,
    private val appEnvironment: AppDevEnvironment,
    private val socketTagger: SocketTagger,
    private val hostConnection: BroadcastNetworkStatus.ConnectionInfo.Connected,
    private val port: Int,
    private val yoloRepeatDelay: Duration,
    private val session: ProxySession<TcpProxyData>,
    proxyType: SharedProxy.Type,
    serverStopConsumer: EventConsumer<ServerStopRequestEvent>,
    enforcer: ThreadEnforcer,
    serverDispatcher: ServerDispatcher
) :
    BaseProxyManager<ServerSocket>(
        proxyType = proxyType,
        enforcer = enforcer,
        serverDispatcher = serverDispatcher,
        serverStopConsumer = serverStopConsumer,
    ) {

  /** Keep track of how many times we fail to claim a socket in a row. */
  private val proxyFailCount = MutableStateFlow(0)

  @CheckResult
  private fun resolveHostNameOrIpAddress(connection: Socket): String {
    val remote = connection.remoteAddress
    if (remote !is InetSocketAddress) {
      warnLog { "Block non-internet socket addresses, we expect clients to be inet: $connection" }
      return ""
    }

    return remote.hostname
  }

  private suspend fun CoroutineScope.handleProxyConnection(
      networkBinder: SocketBinder.NetworkBinder,
      proxyInput: ByteReadChannel,
      proxyOutput: ByteWriteChannel,
      hostNameOrIp: String,
      socketTracker: SocketTracker,
  ) {
    // Resolve the client as an IP or hostname
    if (hostNameOrIp.isBlank()) {
      warnLog { "Unable to resolve TetherClient for connection" }
      return
    }

    try {
      session.exchange(
          scope = this,
          networkBinder = networkBinder,
          hostConnection = hostConnection,
          serverDispatcher = serverDispatcher,
          socketTracker = socketTracker,
          data =
              TcpProxyData(
                  proxyInput = proxyInput,
                  proxyOutput = proxyOutput,
                  hostNameOrIp = hostNameOrIp,
              ),
      )
    } finally {
      proxyOutput.flush()
    }
  }

  /**
   * This function must ALWAYS call connection.usingConnection {} or else a socket may potentially
   * leak
   */
  private suspend fun runSession(
      scope: CoroutineScope,
      networkBinder: SocketBinder.NetworkBinder,
      connection: Socket,
      socketTracker: SocketTracker,
  ) {
    val hostNameOrIp = resolveHostNameOrIpAddress(connection)
    try {
      // Sometimes, this can fail because of a broken pipe
      // Catch the error and continue
      connection.usingConnection(autoFlush = true) { proxyInput, proxyOutput ->
        scope.handleProxyConnection(
            networkBinder = networkBinder,
            proxyInput = proxyInput,
            proxyOutput = proxyOutput,
            hostNameOrIp = hostNameOrIp,
            socketTracker = socketTracker,
        )
      }
    } catch (e: Throwable) {
      e.ifNotCancellation {
        if (e is SocketTimeoutException) {
          warnLog { "Proxy:Server socket timeout! $hostNameOrIp" }
        } else {
          errorLog(e) { "Error occurred while establishing TCP Proxy Connection: $hostNameOrIp" }
        }
      }
    }
  }

  override suspend fun openServer(builder: SocketBuilder): ServerSocket =
      withContext(context = serverDispatcher.primary) {
        val localAddress =
            getServerAddress(
                hostName = hostConnection.hostName,
                port = port,
                verifyPort = true,
                verifyHostName = true,
            )
        debugLog { "Bind TCP server to local address: $localAddress" }
        return@withContext builder
            .tcp()
            .configure {
              reuseAddress = true
              reusePort = true
            }
            .also { socketTagger.tagSocket() }
            .bind(localAddress = localAddress)
      }

  private suspend fun prepareToTryAgainOrThrow(e: IOException) {
    errorLog(e) { "We've caught an IOException opening the ServerSocket!" }
    val failCount = proxyFailCount.value
    val canTryAgain = failCount < PROXY_ACCEPT_TOO_MANY_FAILURES
    if (canTryAgain) {
      proxyFailCount.update { it + 1 }
      debugLog { "In YOLO mode, we ignore IOException and just try again. Yolo!: $failCount" }

      // Wait just a little bit
      delay(yoloRepeatDelay)
    } else {
      // Reset back to zero
      proxyFailCount.value = 0

      // Otherwise, we treat this error as a no-no
      warnLog { "Too many IOExceptions thrown, even for YOLO mode :(: $failCount" }
      throw IOException("Too many failed connection attempts: $failCount", e)
    }
  }

  @CheckResult
  private suspend fun ensureAcceptedConnection(server: ServerSocket): Socket {
    while (!server.isClosed) {
      try {
        if (appEnvironment.isYoloError.first()) {
          warnLog { "In YOLO mode, we simulate an IOException" }
          throw IOException("YOLO Mode Test Error!")
        }

        // This can fail with an IOException
        // No idea why (Java things)
        // but KTOR seems to fix this by just "ignoring" the problem and trying again
        // so that's what we do in YOLO mode
        // https://github.com/ktorio/ktor/commit/634ffb3e6ae07e2979af16a42ce274aca1407cf9
        return server.accept().also {
          // We got a socket, yay!
          proxyFailCount.value = 0
        }
      } catch (e: IOException) {
        // If we are in YOLO mode and under the fail count limit, we can swallow the error and
        // try to accept again.
        //
        // Otherwise this function will throw, which will break out of the loop and stop the server
        prepareToTryAgainOrThrow(e)
      }
    }

    // How did you get here?
    throw IllegalStateException("TCP Proxy failed to grab a socket correctly")
  }

  override suspend fun runServer(tracker: SocketTracker, server: ServerSocket) =
      withContext(context = serverDispatcher.primary) {
        val addr = server.localAddress
        debugLog { "Awaiting TCP connections on $addr" }

        socketBinder.withMobileDataNetworkActive { networkBinder ->
          try {
            // In a loop, we wait for new TCP connections and then offload them to their own
            // routine.
            while (!server.isClosed) {
              // We must close the connection in the launch{} after exchange is over
              //
              // If this function throws, the server will stop
              val connection = ensureAcceptedConnection(server)

              // Run this server loop off thread so we can handle multiple connections at once.
              launch(context = serverDispatcher.primary) {
                try {
                  // Track this socket to close it later
                  tracker.track(connection)

                  runSession(
                      scope = this,
                      networkBinder = networkBinder,
                      connection = connection,
                      socketTracker = tracker,
                  )
                } catch (e: Throwable) {
                  e.ifNotCancellation { errorLog(e) { "Error during server socket accept" } }
                } finally {
                  connection.dispose()
                }
              }
            }
          } finally {
            debugLog { "Closing TCP server $addr" }
          }
        }
      }

  override suspend fun onServerClosing() {
    // Blank for now
  }

  companion object {

    /** If we fail to claim a socket this many times in a row, just assume we are dead. */
    private const val PROXY_ACCEPT_TOO_MANY_FAILURES = 10
  }
}
