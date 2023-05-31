/*
 * Copyright 2023 pyamsoft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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
import com.pyamsoft.tetherfi.server.ServerInternalApi
import com.pyamsoft.tetherfi.server.clients.BlockedClients
import com.pyamsoft.tetherfi.server.clients.SeenClients
import com.pyamsoft.tetherfi.server.clients.TetherClient
import com.pyamsoft.tetherfi.server.event.ProxyRequest
import com.pyamsoft.tetherfi.server.proxy.session.DestinationInfo
import com.pyamsoft.tetherfi.server.proxy.session.ProxySession
import com.pyamsoft.tetherfi.server.proxy.session.tagSocket
import com.pyamsoft.tetherfi.server.urlfixer.UrlFixer
import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.cancel
import io.ktor.utils.io.close
import io.ktor.utils.io.joinTo
import io.ktor.utils.io.readUTF8Line
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.net.URI
import java.time.Clock
import java.time.LocalDateTime
import javax.inject.Inject

internal class TcpProxySession
@Inject
internal constructor(
    /** Need to use MutableSet instead of Set because of Java -> Kotlin fun. */
    @ServerInternalApi private val urlFixers: MutableSet<UrlFixer>,
    @ServerInternalApi private val dispatcher: CoroutineDispatcher,
    private val blockedClients: BlockedClients,
    private val seenClients: SeenClients,
    private val clock: Clock,
    private val enforcer: ThreadEnforcer,
) : ProxySession<TcpProxyData> {

  /**
   * Parse the first line of content which may be a connect call
   *
   * We must do this unbuffered because we only want the first line to determine which host and what
   * port we are connecting on
   *
   * If we buffer we may end up reading the whole input which can be huge, and OOM us.
   */
  @CheckResult
  private suspend fun parseRequest(input: ByteReadChannel): ProxyRequest? {
    /**
     * Read the first line as it should include enough data for us yeah ok, there is often extra
     * data sent over but its mainly for optimization and rarely required to actually make a
     * connection
     */
    val line = input.readUTF8Line()

    // No line, no go
    if (line.isNullOrBlank()) {
      Timber.w("No input read from proxy")
      return null
    }

    try {
      // Given the line, it needs to be in an expected format or we can't do it
      val methodData = getMethodAndUrlString(line)
      if (methodData == null) {
        Timber.w("Unable to parse method and URL: $line")
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
    } catch (e: Throwable) {
      e.ifNotCancellation {
        Timber.e(e, "Unable to parse request: $line")
        return null
      }
    }
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
                Timber.w(
                    "Port string was not a valid port: $possiblyProtocolAndHostAndPort => $portString")
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
   * Some connection request formats are buggy, this method seeks to fix them to what it knows in
   * very specific cases is correct
   */
  @CheckResult
  private fun String.fixSpecialBuggyUrls(): String {
    var result = this
    for (fixer in urlFixers) {
      result = fixer.fix(result)
    }
    return result
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
      Timber.w("Invalid request line format. No space: '$line'")
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
      Timber.w("Invalid request line format. No nextSpace: '$line' => '$restOfLine'")
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

  private suspend fun talk(input: ByteReadChannel, output: ByteWriteChannel) {
    //    KtorDefaultPool.useInstance { buffer ->
    //      while (isActive) {
    //        val array = buffer.array()
    //        val size = input.readAvailable(array)
    //        if (size < 0) {
    //          break
    //        }
    //
    //        output.writeFully(array, 0, size)
    //      }
    //    }

    // Should be faster than parsing byte buffers raw
    input.joinTo(output, closeOnEnd = true)
  }

  private suspend fun exchangeInternet(
      scope: CoroutineScope,
      proxyInput: ByteReadChannel,
      proxyOutput: ByteWriteChannel,
      internetInput: ByteReadChannel,
      internetOutput: ByteWriteChannel,
      request: ProxyRequest,
  ) {
    enforcer.assertOffMainThread()

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
          scope.launch(context = dispatcher) {
            enforcer.assertOffMainThread()

            // Send data from the internet back to the proxy in a different thread
            talk(
                input = internetInput,
                output = proxyOutput,
            )
          }

      // Send input from the proxy (clients) to the internet on this thread
      talk(
          input = proxyInput,
          output = internetOutput,
      )

      // Wait for internet communication to finish
      job.join()
    } catch (e: Throwable) {
      e.ifNotCancellation {
        Timber.e(e, "Error during Internet exchange")
        writeError(proxyOutput)
      }
    }
  }

  /**
   * Given the initial proxy request, connect to the Internet from our device via the connected
   * socket
   */
  @CheckResult
  private suspend fun connectToInternet(request: ProxyRequest): Socket {
    enforcer.assertOffMainThread()

    // Tag sockets for Android O strict mode
    tagSocket()

    // We dont actually use the socket tls() method here since we are not a TLS server
    // We do the CONNECT based workaround to handle HTTPS connections

    val remote =
        InetSocketAddress(
            hostname = request.host,
            port = request.port,
        )

    val rawSocket = aSocket(ActorSelectorManager(context = dispatcher))
    return rawSocket.tcp().connect(remoteAddress = remote)
  }

  @CheckResult
  private fun resolveClient(connection: Socket): TetherClient? {
    val remote = connection.remoteAddress
    if (remote !is InetSocketAddress) {
      Timber.w("Block non-internet socket addresses, we expect clients to be inet: $connection")
      return null
    }

    val hostNameOrIp = remote.hostname
    return if (IP_ADDRESS_REGEX.matches(hostNameOrIp)) {
      TetherClient.IpAddress(
          ip = hostNameOrIp,
          firstSeen = LocalDateTime.now(clock),
      )
    } else {
      TetherClient.HostName(
          hostname = hostNameOrIp,
          firstSeen = LocalDateTime.now(clock),
      )
    }
  }

  @CheckResult
  private fun isBlockedClient(client: TetherClient): Boolean {
    return blockedClients.isBlocked(client)
  }

  private fun CoroutineScope.handleClientRequestSideEffects(client: TetherClient) {
    enforcer.assertOffMainThread()

    // Mark all client connections as seen
    //
    // We need to do this because we have access to the MAC address via the GroupInfo.clientList
    // but not the IP address. Android does not let us access the system ARP table so we cannot map
    // MACs to IPs. Thus we need to basically hold our own table of "known" IP addresses and allow
    // a user to block them as they see fit. This is UX wise, not great at all, since a user must
    // eliminate a "bad" IP address by first knowing all the good ones.
    //
    // Though, arguably, blocking is only a nice to have. Real network security should be handled
    // via the password.
    launch(context = dispatcher) {
      enforcer.assertOffMainThread()

      // We launch to have this side effect run in parallel with processing
      seenClients.seen(client)
    }
  }

  private suspend fun CoroutineScope.proxyToInternet(
      request: ProxyRequest,
      proxyInput: ByteReadChannel,
      proxyOutput: ByteWriteChannel,
  ) {
    enforcer.assertOffMainThread()

    // Given the request, connect to the Web
    try {
      connectToInternet(request).use { internet ->
        val internetInput = internet.openReadChannel()
        val internetOutput = internet.openWriteChannel(autoFlush = true)

        try {
          // Communicate between the web connection we've made and back to our client device
          exchangeInternet(
              scope = this,
              proxyInput = proxyInput,
              proxyOutput = proxyOutput,
              internetInput = internetInput,
              internetOutput = internetOutput,
              request = request,
          )
        } finally {
          withContext(context = NonCancellable) {
            internetInput.cancel()
            internetOutput.close()
          }
        }
      }
    } catch (e: Throwable) {
      e.ifNotCancellation {
        Timber.e(e, "Error during connect to internet: $request")
        writeError(proxyOutput)
      }
    }
  }

  private suspend fun handleClientRequest(
      scope: CoroutineScope,
      proxyInput: ByteReadChannel,
      proxyOutput: ByteWriteChannel,
      client: TetherClient
  ) {
    // This is launched as its own scope so that the side effect does not slow
    // down the internet traffic processing.
    // Since this context is our own dispatcher which is cachedThreadPool backed,
    // we just "spin up" another thread and forget about it performance wise.
    scope.launch(context = dispatcher) {
      enforcer.assertOffMainThread()

      handleClientRequestSideEffects(client)
    }

    // If the client is blocked we do not process any inpue
    if (isBlockedClient(client)) {
      Timber.w("Client is marked blocked: $client")
      writeError(proxyOutput)
      return
    }

    // We use a string parsing to figure out what this HTTP request wants to do
    val request = parseRequest(proxyInput)
    if (request == null) {
      Timber.w("Could not parse proxy request")
      writeError(proxyOutput)
      return
    }

    // And then we go to the web!
    scope.proxyToInternet(
        request = request,
        proxyInput = proxyInput,
        proxyOutput = proxyOutput,
    )
  }

  override suspend fun exchange(
      scope: CoroutineScope,
      data: TcpProxyData,
  ) =
      withContext(context = dispatcher) {
        enforcer.assertOffMainThread()

        /** The Proxy is our device */
        /** The Proxy is our device */
        val connection = data.connection
        val proxyInput = connection.openReadChannel()
        val proxyOutput = connection.openWriteChannel(autoFlush = true)

        try {
          // Resolve the client as an IP or hostname
          val client = resolveClient(connection)
          if (client == null) {
            Timber.w("Unable to resolve TetherClient for connection: $connection")
            writeError(proxyOutput)
            return@withContext
          }

          handleClientRequest(
              scope = this,
              client = client,
              proxyInput = proxyInput,
              proxyOutput = proxyOutput,
          )
        } catch (e: Throwable) {
          e.ifNotCancellation { Timber.e(e, "Error during TCP exchange: $connection") }
        } finally {
          withContext(context = NonCancellable) {
            proxyInput.cancel()
            proxyOutput.close()
          }
        }
      }

  private data class MethodData(
      val url: String,
      val method: String,
      val version: String,
  )

  companion object {

    /**
     * What the fuck is this
     * https://stackoverflow.com/questions/10006459/regular-expression-for-ip-address-validation
     *
     * Tests if a given string is an IP address
     */
    private val IP_ADDRESS_REGEX =
        """^(\d|[1-9]\d|1\d\d|2([0-4]\d|5[0-5]))\.(\d|[1-9]\d|1\d\d|2([0-4]\d|5[0-5]))\.(\d|[1-9]\d|1\d\d|2([0-4]\d|5[0-5]))\.(\d|[1-9]\d|1\d\d|2([0-4]\d|5[0-5]))$""".toRegex()

    private const val LINE_ENDING = "\r\n"

    /**
     * Check if this is an HTTPS connection
     *
     * The only HTTPS connection we can read is the CONNECT call
     */
    @CheckResult
    private fun isHttpsConnection(input: ProxyRequest): Boolean {
      return input.method == "CONNECT"
    }

    /** Write a generic error back to the client socket because something has gone wrong */
    private suspend fun writeError(output: ByteWriteChannel) {
      proxyResponse(output, "HTTP/1.1 502 Bad Gateway")
    }

    /**
     * Respond to the client with a message string
     *
     * Properly line-ended with flushed output
     */
    private suspend fun proxyResponse(output: ByteWriteChannel, response: String) {
      output.apply {
        writeFully(writeMessageAndAwaitMore(response))
        writeFully(LINE_ENDING.encodeToByteArray())
        flush()
      }
    }

    /**
     * Convert a message string into a byte array
     *
     * Correctly end the line with return and newline
     */
    @CheckResult
    private fun writeMessageAndAwaitMore(message: String): ByteArray {
      val msg = if (message.endsWith(LINE_ENDING)) message else "${message}$LINE_ENDING"
      return msg.encodeToByteArray()
    }
  }
}
