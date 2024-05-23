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

package com.pyamsoft.tetherfi.server.proxy.session.tcp

import com.pyamsoft.pydroid.core.ThreadEnforcer
import com.pyamsoft.pydroid.util.ifNotCancellation
import com.pyamsoft.tetherfi.core.Timber
import com.pyamsoft.tetherfi.server.clients.ByteTransferReport
import com.pyamsoft.tetherfi.server.event.ProxyRequest
import com.pyamsoft.tetherfi.server.proxy.ServerDispatcher
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.readUTF8Line
import io.ktor.utils.io.writeFully
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class HttpTcpTransport
@Inject
internal constructor(
    private val requestParser: RequestParser,
    private val enforcer: ThreadEnforcer,
) : TcpSessionTransport {

  /**
   * HTTPS Connections are encrypted and so we cannot see anything further past the initial CONNECT
   * call.
   *
   * Establish the connection to a site and then continue processing the connection data
   */
  private suspend fun establishHttpsConnection(
      input: ByteReadChannel,
      output: ByteWriteChannel,
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

    proxyResponse(output, "HTTP/1.1 200 Connection Established")
  }

  /**
   * Send the first communication request
   *
   * This was not an HTTPS CONNECT request, so we just pass it along to our HTTP client
   */
  private suspend fun replayHttpCommunication(
      output: ByteWriteChannel,
      request: ProxyRequest,
  ) {
    Timber.d { "Rewrote initial HTTP request: ${request.raw} -> ${request.httpRequest}" }
    output.writeFully(writeMessageAndAwaitMore(request.httpRequest))
  }

  /**
   * Parse the first line of content which may be a connect call
   *
   * We must do this unbuffered because we only want the first line to determine which host and what
   * port we are connecting on
   *
   * If we buffer we may end up reading the whole input which can be huge, and OOM us.
   */
  override suspend fun parseRequest(input: ByteReadChannel): ProxyRequest? {
    val line = input.readUTF8Line()
    Timber.d { "Proxy input: $line" }

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
      request: ProxyRequest,
  ): ByteTransferReport? {
    enforcer.assertOffMainThread()

    // We use MSF here even though we aren't reacting because how
    // else do I get a Kotlin native atomic?
    val report =
        MutableStateFlow(
            ByteTransferReport(
                proxyToInternet = 0UL,
                internetToProxy = 0UL,
            ),
        )

    try {
      if (request.isHttpsConnectRequest()) {
        // Establish an HTTPS connection by faking the CONNECT response
        // Send a 200 to the connecting client so that they will then continue to
        // send the actual HTTP data to the real endpoint
        establishHttpsConnection(proxyInput, proxyOutput)
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
            val totalBytes =
                talk(
                    input = internetInput,
                    output = proxyOutput,
                )

            // Save as report
            // MSF shouldn't need a mutex and this operation touches an exclusive field,
            // we should be okay
            report.update {
              it.copy(
                  internetToProxy = it.internetToProxy + totalBytes.toULong(),
              )
            }
          }

      // Send input from the proxy (clients) to the internet on this thread
      val totalBytes =
          talk(
              input = proxyInput,
              output = internetOutput,
          )

      // Save as report
      // MSF shouldn't need a mutex and this operation touches an exclusive field,
      // we should be okay
      report.update {
        it.copy(
            proxyToInternet = it.proxyToInternet + totalBytes.toULong(),
        )
      }

      // Wait for internet communication to finish
      job.join()

      // And deliver!
      return report.value
    } catch (e: Throwable) {
      e.ifNotCancellation { writeError(proxyOutput) }

      // Error means no report
      return null
    }
  }
}
