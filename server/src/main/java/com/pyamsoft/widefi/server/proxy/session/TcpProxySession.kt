package com.pyamsoft.widefi.server.proxy.session

import androidx.annotation.CheckResult
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.core.Enforcer
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.pydroid.util.ifNotCancellation
import com.pyamsoft.widefi.server.event.ConnectionEvent
import com.pyamsoft.widefi.server.event.ErrorEvent
import com.pyamsoft.widefi.server.event.ProxyRequest
import com.pyamsoft.widefi.server.proxy.SharedProxy
import com.pyamsoft.widefi.server.proxy.session.mempool.MemPool
import com.pyamsoft.widefi.server.proxy.tagSocket
import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.readAvailable
import io.ktor.utils.io.readUTF8Line
import io.ktor.utils.io.writeFully
import java.net.URI
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

internal class TcpProxySession
internal constructor(
    private val dispatcher: CoroutineDispatcher,
    private val errorBus: EventBus<ErrorEvent>,
    private val connectionBus: EventBus<ConnectionEvent>,
    private val memPool: MemPool<ByteArray>,
    private val urlFixers: Set<UrlFixer>,
    proxyDebug: Boolean,
) : BaseProxySession<Socket>(SharedProxy.Type.TCP, proxyDebug) {

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
    val line = input.readUTF8Line()

    if (line.isNullOrBlank()) {
      warnLog { "No input read from proxy" }
      return null
    }

    val methodData = getMethodAndUrlString(line)
    if (methodData == null) {
      warnLog { "Unable to parse method and URL: $line" }
      return null
    }

    return try {
      val urlData = getUrlAndPort(methodData.url)
      ProxyRequest(
          url = methodData.url,
          host = urlData.host,
          method = methodData.method,
          port = urlData.port,
          version = methodData.version,
          raw = line,
      )
    } catch (e: Throwable) {
      warnLog { "Unable to parse url and port: $methodData" }
      null
    }
  }

  /**
   * Figure out the URL and the port of the connection
   *
   * If the URL does not include the port, determine it from the protocol or just assume it is HTTP
   */
  @CheckResult
  private fun getUrlAndPort(possiblyProtocolAndHostAndPort: String): UrlData {
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
      // Well that didn't work
      val portSeperator = possiblyProtocolAndHostAndPort.indexOf(':')
      val possiblyProtocolAndHost: String

      if (portSeperator >= 0) {
        possiblyProtocolAndHost = possiblyProtocolAndHostAndPort.substring(0, portSeperator)
        val portString = possiblyProtocolAndHostAndPort.substring(portSeperator + 1)
        val maybePort = portString.toIntOrNull()
        port =
            if (maybePort == null) {
              warnLog {
                "Port string was not a valid port: $possiblyProtocolAndHostAndPort => $portString"
              }
              // Default to port 80 for HTTP
              80
            } else {
              maybePort
            }
      } else {
        // No port in the URL, this is the URL then
        possiblyProtocolAndHost = possiblyProtocolAndHostAndPort
      }

      val splitByProtocol = possiblyProtocolAndHost.split("://")

      // Strip the protocol of http:// off of the url, but if there is no protocol, we just have
      // the
      // host name as the entire thing

      // Could be a name like mywebsite.com/filehere.html and we only want the host name
      // mywebsite.com
      val hostAndPossiblyFile = splitByProtocol[if (splitByProtocol.size == 1) 0 else 1]
      val fileIndex = hostAndPossiblyFile.indexOf("/")
      host =
          if (fileIndex >= 0) hostAndPossiblyFile.substring(0, fileIndex) else hostAndPossiblyFile

      // Guess the protocol or assume it empty
      protocol = if (splitByProtocol.size == 1) splitByProtocol[0] else ""
    }

    if (port < 0) {
      // Port is guessed based on URL
      port = if (protocol.startsWith("https")) 443 else 80
    }

    return UrlData(
        // Just in-case we missed a slash, a name with a slash is not a valid hostname
        // its actually a host with a file path of ROOT, which is bad
        host = host.trimEnd('/'),
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
    val firstSpace = line.indexOf(' ')
    if (firstSpace < 0) {
      warnLog { "Invalid request line format. No space: '$line'" }
      return null
    }

    val restOfLine = line.substring(firstSpace + 1)
    val nextSpace = restOfLine.indexOf(' ')
    if (nextSpace < 0) {
      warnLog { "Invalid request line format. No nextSpace: '$line' => '$restOfLine'" }
      return null
    }

    val methodString = line.substring(0, firstSpace)
    val urlString = restOfLine.substring(0, nextSpace)
    val versionString = restOfLine.substring(nextSpace + 1)
    return MethodData(
        url = urlString.trim().fixSpecialBuggyUrls(),
        method = methodString.trim(),
        version = versionString.trim(),
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
   * HTTPS Connections are encrypted and so we cannot see anything further past the initial CONNECT
   * call.
   *
   * Establish the connection to a site and then continue processing the connection data
   */
  private suspend fun CoroutineScope.establishHttpsConnection(
      input: ByteReadChannel,
      output: ByteWriteChannel,
      request: ProxyRequest,
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

    debugLog { "Establish HTTPS ACK connection: $request" }
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

    debugLog { "Replay initial HTTP connection: $request => $newRequest" }
    output.writeFully(writeMessageAndAwaitMore(newRequest))
  }

  private suspend fun CoroutineScope.talk(input: ByteReadChannel, output: ByteWriteChannel) {
    memPool.use { buffer ->
      while (isActive) {
        val size = input.readAvailable(buffer)
        if (size < 0) {
          break
        }

        debugLog { "TALK: $input -> $output\n${String(buffer, 0, size)}" }
        output.writeFully(buffer, 0, size)
      }
    }
  }

  private suspend fun exchangeInternet(
      proxyInput: ByteReadChannel,
      proxyOutput: ByteWriteChannel,
      internetInput: ByteReadChannel,
      internetOutput: ByteWriteChannel,
      request: ProxyRequest,
  ) = coroutineScope {
    try {
      if (isHttpsConnection(request)) {
        // Establish an HTTPS connection by faking the CONNECT response
        // Send a 200 to the connecting client so that they will then continue to
        // send the actual HTTP data to the real endpoint
        establishHttpsConnection(proxyInput, proxyOutput, request)
      } else {
        // Send initial HTTP communication, since we consumed it above
        replayHttpCommunication(
            output = internetOutput,
            request = request,
        )
      }

      // Exchange data until completed
      debugLog { "Exchange rest of data: $request" }
      val job =
          launch(context = dispatcher) {
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
      debugLog { "Done with proxy request: $request" }
    } catch (e: Throwable) {
      e.ifNotCancellation {
        Timber.e(e, "${proxyType.name} Error during Internet exchange")
        errorBus.send(
            ErrorEvent.Tcp(
                request = request,
                throwable = e,
            ),
        )
        writeError(proxyOutput)
      }
    }
  }

  override suspend fun exchange(proxy: Socket) {
    Enforcer.assertOffMainThread()

    val proxyInput = proxy.openReadChannel()
    val proxyOutput = proxy.openWriteChannel(autoFlush = true)

    val request = parseRequest(proxyInput)

    try {
      if (request == null) {
        val msg = "Could not parse proxy request"
        Timber.w("${proxyType.name} $msg")
        errorBus.send(
            ErrorEvent.Tcp(
                request = request,
                throwable = RuntimeException(msg),
            ),
        )
        writeError(proxyOutput)
        return
      }

      debugLog { "${proxyType.name} Proxy to: $request" }

      connectToInternet(request).use { internet ->
        debugLog { "Connect to internet: $request" }

        // Log connection
        Timber.d("Log connection: $request")
        connectionBus.send(
            ConnectionEvent.Tcp(
                request = request,
            ),
        )

        val internetInput = internet.openReadChannel()
        val internetOutput = internet.openWriteChannel(autoFlush = true)
        exchangeInternet(
            proxyInput = proxyInput,
            proxyOutput = proxyOutput,
            internetInput = internetInput,
            internetOutput = internetOutput,
            request = request,
        )
      }
    } catch (e: Throwable) {
      e.ifNotCancellation {
        Timber.e(e, "${proxyType.name} Error during connect to internet: $request")
        errorBus.send(
            ErrorEvent.Tcp(
                request = request,
                throwable = e,
            ),
        )
        writeError(proxyOutput)
      }
    }
  }

  private suspend fun connectToInternet(request: ProxyRequest): Socket {
    // Tag sockets for Android O strict mode
    tagSocket()

    // We dont actually use the socket tls() method here since we are not a TLS server
    // We do the CONNECT based workaround to handle HTTPS connections

    val remote =
        InetSocketAddress(
            hostname = request.host,
            port = request.port,
        )

    return aSocket(ActorSelectorManager(context = dispatcher))
        .tcp()
        .connect(
            remoteAddress = remote,
        )
  }

  companion object {

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
  }

  private data class MethodData(
      val url: String,
      val method: String,
      val version: String,
  )

  private data class UrlData(
      val host: String,
      val port: Int,
  )
}
