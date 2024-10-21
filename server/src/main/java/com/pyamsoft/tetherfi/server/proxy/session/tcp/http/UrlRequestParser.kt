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

import androidx.annotation.CheckResult
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.pydroid.util.ifNotCancellation
import com.pyamsoft.tetherfi.core.Timber
import com.pyamsoft.tetherfi.server.ServerInternalApi
import com.pyamsoft.tetherfi.server.proxy.session.tcp.http.urlfixer.UrlFixer
import java.net.URL
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
      override val method: String,
      val url: String,
      val version: String,
  ) : TunnelRequest(method)

  private data class DestinationInfo(
      val hostName: String,
      val port: Int,
      val file: String,
      val proto: String
  )

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
  private fun getUrlAndPort(methodData: MethodData): DestinationInfo? {

    // This could be anything like the following
    // protocol://hostname:port
    //
    // HTTPS CONNECT requests will always have a host and a port, no protocol
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
    val possiblyProtocolAndHostAndPort = methodData.url

    // If we are missing the protocol, just assume we are HTTP
    val hopefullyValidUrl =
        if (hasProtocol(possiblyProtocolAndHostAndPort)) possiblyProtocolAndHostAndPort
        else {
          if (methodData.isHttpsConnectRequest()) {
            // For connect requests, we assume https:// as according to RFC spec
            // https://www.rfc-editor.org/rfc/rfc9110#CONNECT
            "https://$possiblyProtocolAndHostAndPort"
          } else {
            Timber.w { "No protocol provided, assume HTTP: $possiblyProtocolAndHostAndPort" }
            "http://$possiblyProtocolAndHostAndPort"
          }
        }

    try {
      // Just try with the normal java URI parser
      val url = URL(hopefullyValidUrl)

      // These have to be here
      val host = url.host.requireNotNull()
      val protocol = url.protocol.requireNotNull()

      // Can be -1, but always present
      val port: Int
      if (url.port >= 0) {
        port = url.port
      } else if (url.defaultPort >= 0) {
        port = url.defaultPort
      } else {
        Timber.w { "No port provided and no default port for protocol: $hopefullyValidUrl" }
        // Default to port 80
        port = 80
      }

      // Return the path and the query
      var file = url.file

      // Add the fragment if one exists
      url.ref?.also { file += "#$it" }

      // Sometimes the file can be empty, like it the path was not included,
      // or is ROOT. When this happens, be sure to prepend the / to the file
      if (!file.startsWith("/")) {
        file = "/$file"
      }

      return DestinationInfo(
          // Just in-case we missed a slash, a name with a slash is not a valid hostname
          // its actually a host with a file path of ROOT, which is bad
          hostName = host.trimEnd('/'),
          port = port,
          file = file,
          proto = protocol,
      )
    } catch (e: Throwable) {
      return null
    }
  }

  override fun parse(line: String): HttpProxyRequest? {
    try {
      val methodData = getMethodAndUrlString(line)
      if (methodData == null) {
        Timber.w { "Unable to parse method and URL: $line" }
        return null
      }

      val urlData = getUrlAndPort(methodData)
      if (urlData == null) {
        Timber.w { "Unable to parse URL information: $line $methodData" }
        return null
      }

      return HttpProxyRequest(
              raw = line,
              method = methodData.method,
              host = urlData.hostName,
              port = urlData.port,
              version = methodData.version,
              file = urlData.file,
          )
          .also { Timber.d { "Proxy Request: $it" } }
    } catch (e: Throwable) {
      e.ifNotCancellation {
        Timber.e(e) { "Unable to parse request: $line" }
        return null
      }
    }
  }

  companion object {

    /** If this string already has a valid HTTP protocol we don't need to attach it */
    @JvmStatic
    @CheckResult
    private fun hasProtocol(url: String): Boolean {
      return url.startsWith("http://") || url.startsWith("https://")
    }
  }
}
