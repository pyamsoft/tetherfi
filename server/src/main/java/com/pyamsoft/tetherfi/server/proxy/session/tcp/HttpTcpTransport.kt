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

import androidx.annotation.CheckResult
import com.pyamsoft.pydroid.core.ThreadEnforcer
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.pydroid.util.ifNotCancellation
import com.pyamsoft.tetherfi.core.Timber
import com.pyamsoft.tetherfi.server.ServerInternalApi
import com.pyamsoft.tetherfi.server.clients.ByteTransferReport
import com.pyamsoft.tetherfi.server.event.ProxyRequest
import com.pyamsoft.tetherfi.server.proxy.ServerDispatcher
import com.pyamsoft.tetherfi.server.proxy.session.DestinationInfo
import com.pyamsoft.tetherfi.server.urlfixer.UrlFixer
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.readUTF8Line
import io.ktor.utils.io.writeFully
import java.net.URI
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal class HttpTcpTransport
@Inject
internal constructor(
    /** Need to use MutableSet instead of Set because of Java -> Kotlin fun. */
    @ServerInternalApi private val urlFixers: MutableSet<UrlFixer>,
    private val enforcer: ThreadEnforcer,
) :
    BaseTcpSessionTransport(
        urlFixers = urlFixers,
    ) {

  /**
   * Check if this is an HTTPS connection
   *
   * The only HTTPS connection we can read is the CONNECT call
   */
  @CheckResult
  private fun isHttpsConnection(input: ProxyRequest): Boolean {
    return input.method == "CONNECT"
  }

  /**
   * Figure out the URL and the port of the connection
   *
   * If the URL does not include the port, determine it from the protocol or just assume it is HTTP
   */
  @CheckResult
  private fun getUrlAndPort(possiblyProtocolAndHostAndPort: String): DestinationInfo {
    var host: String
    var protocol: String
    var port = -1

    // This could be anything like the following
    // protocol://hostname:port
    //
    // http://example.com -> http example.com 80
    // http://example.com:69 -> http example.com 69
    // example.com:80 -> http example.com 80
    // example.com:443 -> https example.com 443
    // example.com -> http example.com 80
    // https://example.com -> https example.com 443
    // https://example.com/file.html -> https example.com 443
    // example.com:443/file.html -> https example.com 443
    // example.com/file.html -> http example.com 80
    try {
      // Just try with the normal java URI parser
      val uu = URI(possiblyProtocolAndHostAndPort)

      protocol = uu.scheme.requireNotNull()
      host = uu.host.requireNotNull()
      port = uu.port.requireNotNull()
    } catch (e: Throwable) {
      // Well that didn't work, would have hoped we didn't have to do this but

      // Do we have a port in this URL? if we do split it up
      val possiblyProtocolAndHost: String
      val portSeparator = possiblyProtocolAndHostAndPort.indexOf(':')
      if (portSeparator >= 0) {

        // Split up to just the protocol and host
        possiblyProtocolAndHost = possiblyProtocolAndHostAndPort.substring(0, portSeparator)

        // And then this, should be the port, right?
        val portString = possiblyProtocolAndHostAndPort.substring(portSeparator + 1)

        // Parse the port, or default to just 80 for HTTP traffic
        port =
            portString.toIntOrNull().let { maybePort ->
              if (maybePort == null) {
                Timber.w {
                  "Port string was not a valid port: $possiblyProtocolAndHostAndPort => $portString"
                }
                // Default to port 80 for HTTP
                80
              } else {
                maybePort
              }
            }
      } else {
        // No port in the URL, this is the URL then
        possiblyProtocolAndHost = possiblyProtocolAndHostAndPort
      }

      // Then we split up the protocol
      val splitByProtocol = possiblyProtocolAndHost.split("://")

      // Strip the protocol of http:// off of the url, but if there is no protocol, we just have
      // the host name as the entire thing

      // Could be a name like mywebsite.com/filehere.html and we only want the host name
      // mywebsite.com
      val hostAndPossiblyFile = splitByProtocol[if (splitByProtocol.size == 1) 0 else 1]

      // If there is an additional file attached to this request, ignore it and just grab the URL
      host =
          hostAndPossiblyFile.indexOf("/").let { fileIndex ->
            if (fileIndex >= 0) hostAndPossiblyFile.substring(0, fileIndex) else hostAndPossiblyFile
          }

      // Guess the protocol or assume it empty
      protocol = if (splitByProtocol.size == 1) splitByProtocol[0] else ""
    }

    // If the port was passed but is some random number, guess it from the protocol
    if (port < 0) {
      // And if we don't know the protocol, good old 80
      port = if (protocol.startsWith("https")) 443 else 80
    }

    return DestinationInfo(
        // Just in-case we missed a slash, a name with a slash is not a valid hostname
        // its actually a host with a file path of ROOT, which is bad
        hostName = host.trimEnd('/'),
        port = port,
    )
  }

  /**
   * Figure out the METHOD and URL
   *
   * The METHOD is important because we need to know if this is a CONNECT call for HTTPS
   */
  @CheckResult
  private fun getMethodAndUrlString(line: String): MethodData? {
    // Line is something like
    // CONNECT https://my-internet-domain:80 HTTP/2

    // We find the first space
    val firstSpace = line.indexOf(' ')
    if (firstSpace < 0) {
      Timber.w { "Invalid request line format. No space: '$line'" }
      return null
    }

    // We now have the first space, the start-of-line, and the rest-of-line
    //
    // start-of-line: CONNECT
    // rest-of-line: https://my-internet-domain:80 HTTP/2
    val restOfLine = line.substring(firstSpace + 1)

    // Need to have the last space so we know the version too
    val nextSpace = restOfLine.indexOf(' ')
    if (nextSpace < 0) {
      Timber.w { "Invalid request line format. No nextSpace: '$line' => '$restOfLine'" }
      return null
    }

    // We have everything we need now
    //
    // start-of-line: CONNECT
    // middle-of-line: https://my-internet-domain
    // end-of-line: HTTP/2
    return MethodData(
        url = restOfLine.substring(0, nextSpace).trim().fixSpecialBuggyUrls(),
        method = line.substring(0, firstSpace).trim(),
        version = restOfLine.substring(nextSpace + 1).trim(),
    )
  }

  /**
   * HTTPS Connections are encrypted and so we cannot see anything further past the initial CONNECT
   * call.
   *
   * Establish the connection to a site and then continue processing the connection data
   */
  private suspend fun CoroutineScope.establishHttpsConnection(
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
    } while (isActive && !throwaway.isNullOrBlank())

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
    // Strip off the hostname just leaving file name for requests
    val file = request.url.replace("http://.+\\.\\w+/".toRegex(), "/")

    val newRequest = "${request.method} $file ${request.version}"

    output.writeFully(writeMessageAndAwaitMore(newRequest))
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
    /**
     * Read the first line as it should include enough data for us yeah ok, there is often extra
     * data sent over but its mainly for optimization and rarely required to actually make a
     * connection
     *
     * TODO(Peter): https://github.com/pyamsoft/tetherfi/issues/280
     *              Is reading only a single line causing HTTP issues?
     */
    val line = input.readUTF8Line()

    // No line, no go
    if (line.isNullOrBlank()) {
      Timber.w { "No input read from proxy" }
      return null
    }

    try {
      // Given the line, it needs to be in an expected format or we can't do it
      val methodData = getMethodAndUrlString(line)
      if (methodData == null) {
        Timber.w { "Unable to parse method and URL: $line" }
        return null
      }

      val urlData = getUrlAndPort(methodData.url)
      return ProxyRequest(
              url = methodData.url,
              host = urlData.hostName,
              method = methodData.method,
              port = urlData.port,
              version = methodData.version,
              raw = line,
          )
          .also { Timber.d { "Proxy Request: $it" } }
    } catch (e: Throwable) {
      e.ifNotCancellation {
        Timber.e(e) { "Unable to parse request: $line" }
        return null
      }
    }
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
      if (isHttpsConnection(request)) {
        // Establish an HTTPS connection by faking the CONNECT response
        // Send a 200 to the connecting client so that they will then continue to
        // send the actual HTTP data to the real endpoint
        scope.establishHttpsConnection(proxyInput, proxyOutput)
      } else {
        // Send initial HTTP communication, since we consumed it above
        replayHttpCommunication(
            output = internetOutput,
            request = request,
        )
      }

      // Exchange data until completed
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

  private data class MethodData(
      val url: String,
      val method: String,
      val version: String,
  )
}
