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
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.pydroid.util.ifNotCancellation
import com.pyamsoft.tetherfi.core.Timber
import com.pyamsoft.tetherfi.server.ServerInternalApi
import com.pyamsoft.tetherfi.server.event.ProxyRequest
import com.pyamsoft.tetherfi.server.urlfixer.UrlFixer
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class UrlRequestParser
@Inject
internal constructor(
    /** Need to use MutableSet instead of Set because of Java -> Kotlin fun. */
    @ServerInternalApi private val urlFixers: MutableSet<UrlFixer>,
) : RequestParser {

  private data class MethodData(
      val url: String,
      val method: String,
      val version: String,
  )

  private data class DestinationInfo(
      val hostName: String,
      val port: Int,
      val file: String,
      val isParsedByURIConstructor: Boolean,
      val proto: String
  )

  /**
   * Some connection request formats are buggy, this method seeks to fix them to what it knows in
   * very specific cases is correct
   */
  @CheckResult
  protected fun String.fixSpecialBuggyUrls(): String {
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
   * Figure out the URL and the port of the connection
   *
   * If the URL does not include the port, determine it from the protocol or just assume it is HTTP
   */
  @CheckResult
  private fun getUrlAndPort(possiblyProtocolAndHostAndPort: String): DestinationInfo {
    var host: String
    var protocol: String
    var port = -1
    var file = ""
    var isParsedByURIConstructor: Boolean

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
      file = uu.path.requireNotNull()
      isParsedByURIConstructor = true
    } catch (e: Throwable) {
      Timber.e(e) { "Failed to parse input string by URI constructor" }
      isParsedByURIConstructor = false
      // Well that didn't work, would have hoped we didn't have to do this but

      // Do we have a port in this URL? if we do split it up
      val possiblyProtocolAndHost: String
      val portSeparator = possiblyProtocolAndHostAndPort.indexOf(':')
      if (portSeparator >= 0) {

        // Split up to just the protocol and host
        possiblyProtocolAndHost = possiblyProtocolAndHostAndPort.substring(0, portSeparator)

        // And then this, should be the port, right?
        val possiblyPortStringAndFile = possiblyProtocolAndHostAndPort.substring(portSeparator + 1)

        // If there is a file too, strip it off
        val portString: String
        val fileIndex = possiblyPortStringAndFile.indexOf("/")
        if (fileIndex < 0) {
          // No file, we can straight parse the port
          portString = possiblyPortStringAndFile
        } else {
          // Otherwise, there is a file
          portString = possiblyPortStringAndFile.substring(0, fileIndex)
          file = possiblyPortStringAndFile.substring(fileIndex)
        }

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
      // mywebsite.com and the file name /filehere.html
      val hostAndPossiblyFile = splitByProtocol[if (splitByProtocol.size == 1) 0 else 1]

      // If there is an additional file attached to this request, ignore it and just grab the URL
      val fileIndex = hostAndPossiblyFile.indexOf("/")
      if (fileIndex < 0) {
        host = hostAndPossiblyFile
        // No file
      } else {
        host = hostAndPossiblyFile.substring(0, fileIndex)
        file = hostAndPossiblyFile.substring(fileIndex)
      }

      // Guess the protocol or assume it empty
      protocol = if (splitByProtocol.size == 1) "" else splitByProtocol[0]
    }

    // Guess the protocol
    if (protocol.isBlank()) {
      // If we have no explicit protocol, assume HTTP,
      // UNLESS we are told in advance that it is 443 by port
      if (port == 443) {
        protocol = "https"
      } else {
        protocol = "http"
      }
    }

    // If the port was passed but is some random number, guess it from the protocol
    if (port < 0) {
      // And if we don't know the protocol, good old 80
      port = if (protocol.startsWith("https")) 443 else 80
    }

    // If we parse with the URI constructor, a root path could be a blank line.
    // If so, make it root
    if (file.isBlank()) {
      file = "/"
    }

    return DestinationInfo(
        // Just in-case we missed a slash, a name with a slash is not a valid hostname
        // its actually a host with a file path of ROOT, which is bad
        hostName = host.trimEnd('/'),
        port = port,
        file = file,
        proto = protocol,
        isParsedByURIConstructor = isParsedByURIConstructor,
    )
  }

  override fun parse(line: String): ProxyRequest? {
    try {
      val methodData = getMethodAndUrlString(line)
      if (methodData == null) {
        Timber.w { "Unable to parse method and URL: $line" }
        return null
      }

      val urlData = getUrlAndPort(methodData.url)
      return ProxyRequest(
              raw = line,
              method = methodData.method,
              host = urlData.hostName,
              port = urlData.port,
              version = methodData.version,
              file = urlData.file,
              isParsedByURIConstructor = urlData.isParsedByURIConstructor,
              url = methodData.url,
              proto = urlData.proto,
          )
          .also { Timber.d { "Proxy Request: $it" } }
    } catch (e: Throwable) {
      e.ifNotCancellation {
        Timber.e(e) { "Unable to parse request: $line" }
        return null
      }
    }
  }
}
