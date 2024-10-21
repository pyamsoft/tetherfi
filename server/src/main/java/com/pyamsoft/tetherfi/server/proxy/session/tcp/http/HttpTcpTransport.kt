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

package com.pyamsoft.tetherfi.server.proxy.session.tcp.http

import com.pyamsoft.pydroid.core.ThreadEnforcer
import com.pyamsoft.pydroid.util.ifNotCancellation
import com.pyamsoft.tetherfi.core.Timber
import com.pyamsoft.tetherfi.server.clients.ByteTransferReport
import com.pyamsoft.tetherfi.server.clients.TetherClient
import com.pyamsoft.tetherfi.server.proxy.ServerDispatcher
import com.pyamsoft.tetherfi.server.proxy.session.tcp.TcpSessionTransport
import com.pyamsoft.tetherfi.server.proxy.session.tcp.TransportWriteCommand
import com.pyamsoft.tetherfi.server.proxy.session.tcp.talk
import io.ktor.network.sockets.SocketTimeoutException
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.readUTF8Line
import io.ktor.utils.io.writeFully
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal class HttpTcpTransport
@Inject
internal constructor(
    private val requestParser: RequestParser,
    private val enforcer: ThreadEnforcer,
) : TcpSessionTransport<HttpProxyRequest> {

  /**
   * HTTPS Connections are encrypted and so we cannot see anything further past the initial CONNECT
   * call.
   *
   * Establish the connection to a site and then continue processing the connection data
   */
  private suspend fun establishHttpsConnection(
      input: ByteReadChannel,
      output: ByteWriteChannel,
      request: HttpProxyRequest,
  ) {
    // We exhaust the input here because the client is sending CONNECT data to what it thinks is a
    // server but its actually us, and we don't care how they connect
    //
    // we assume the connect will work and then tell the client so they can start sending the
    // actual data
    var throwaway: String?
    do {
      throwaway = input.readUTF8Line()
    } while (!throwaway.isNullOrBlank())

    Timber.d { "Establish HTTPS CONNECT tunnel ${request.raw}" }
    writeHttpConnectSuccess(output)
  }

  /**
   * Send the first communication request
   *
   * This was not an HTTPS CONNECT request, so we just pass it along to our HTTP client
   */
  private suspend fun replayHttpCommunication(
      output: ByteWriteChannel,
      request: HttpProxyRequest,
  ) {
    // TODO(Peter): If the output socket is already closed this will fail and throw.
    //   realistically, will the output socket ever be closed for the initial connection?
    Timber.d { "Rewrote initial HTTP request: ${request.raw} -> ${request.httpRequest}" }
    output.writeFully(writeHttpMessageAndAwaitMore(request.httpRequest))
  }

  override suspend fun write(proxyOutput: ByteWriteChannel, command: TransportWriteCommand) =
      when (command) {
        TransportWriteCommand.ERROR -> writeProxyHttpError(proxyOutput)
        TransportWriteCommand.BLOCK -> writeHttpClientBlocked(proxyOutput)
      }

  /**
   * Parse the first line of content which may be a connect call
   *
   * We must do this unbuffered because we only want the first line to determine which host and what
   * port we are connecting on
   *
   * If we buffer we may end up reading the whole input which can be huge, and OOM us.
   */
  override suspend fun parseRequest(input: ByteReadChannel): HttpProxyRequest? {
    val line = input.readUTF8Line()

    // No line, no go
    if (line.isNullOrBlank()) {
      Timber.w { "No input read from proxy" }
      return null
    }

    // Given the line, it needs to be in an expected format or we can't do it
    return requestParser.parse(line)
  }

  override suspend fun exchangeInternet(
      scope: CoroutineScope,
      serverDispatcher: ServerDispatcher,
      proxyInput: ByteReadChannel,
      proxyOutput: ByteWriteChannel,
      internetInput: ByteReadChannel,
      internetOutput: ByteWriteChannel,
      request: HttpProxyRequest,
      client: TetherClient,
      onReport: suspend (ByteTransferReport) -> Unit,
  ) {
    enforcer.assertOffMainThread()

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
          while (isActive) {
            delay(5.seconds)
            sendReport()
          }
        }

    try {
      if (request.isHttpsConnectRequest()) {
        // Establish an HTTPS connection by faking the CONNECT response
        // Send a 200 to the connecting client so that they will then continue to
        // send the actual HTTP data to the real endpoint
        establishHttpsConnection(
            input = proxyInput,
            output = proxyOutput,
            request = request,
        )
      } else {
        // Send initial HTTP communication, since we consumed it above
        replayHttpCommunication(
            output = internetOutput,
            request = request,
        )
      }

      // Exchange data until completed
      //
      // For HTTP, this will send over "the rest" of the request
      // For HTTPS, the client "assumes" the CONNECT has succeeded, so there is no more request
      val job =
          scope.launch(context = serverDispatcher.primary) {
            // Send data from the internet back to the proxy in a different thread
            talk(
                client = client,
                input = internetInput,
                output = proxyOutput,
                onCopied = { read ->
                  if (read > 0) {
                    internetToProxyBytes.update { it + read }
                  }
                })
          }

      // Send input from the proxy (clients) to the internet on this thread
      talk(
          client = client,
          input = proxyInput,
          output = internetOutput,
          onCopied = { read ->
            if (read > 0) {
              proxyToInternetBytes.update { it + read }
            }
          })

      // Wait for internet communication to finish
      job.join()
    } catch (e: Throwable) {
      e.ifNotCancellation {
        if (e is SocketTimeoutException) {
          Timber.w { "Proxy:Internet socket timeout! $request $client" }
        } else {
          Timber.e(e) { "Error occurred during internet exchange: $request $client" }
          write(proxyOutput, TransportWriteCommand.ERROR)
        }
      }
    } finally {
      // After we are done, cancel the periodic report and fire one last report
      reportJob.cancel()
      sendReport()
    }
  }
}
