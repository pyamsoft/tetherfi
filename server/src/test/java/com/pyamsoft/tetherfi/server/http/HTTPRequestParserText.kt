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

package com.pyamsoft.tetherfi.server.http

import com.pyamsoft.tetherfi.server.proxy.session.tcp.http.RequestParser
import com.pyamsoft.tetherfi.server.proxy.session.tcp.http.UrlRequestParser
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import org.junit.Test

class HTTPRequestParserText {

  private fun RequestParser.testSuccess(
      line: String,
      port: Int? = null,
      host: String? = null,
      method: String? = null,
      file: String? = null,
      version: String? = null,
  ) {
    return test(
        fails = false,
        line = line,
        port = port,
        host = host,
        method = method,
        file = file,
        version = version,
    )
  }

  private fun RequestParser.testFail(
      line: String,
      port: Int? = null,
      host: String? = null,
      method: String? = null,
      file: String? = null,
      version: String? = null,
  ) {
    return test(
        fails = true,
        line = line,
        port = port,
        host = host,
        method = method,
        file = file,
        version = version,
    )
  }

  private fun RequestParser.test(
      line: String,
      fails: Boolean,
      port: Int? = null,
      host: String? = null,
      method: String? = null,
      file: String? = null,
      version: String? = null,
  ) {
    assertNotEquals(
        true,
        port == null && host == null && method == null && file == null && version == null,
        "You must test at least 1 Request part",
    )

    val result = this.parse(line)
    if (fails) {
      assertNull(result)
    } else {
      assertNotNull(result)
      port?.also { assertEquals(it, result.port) }
      host?.also { assertEquals(it, result.host) }
      method?.also { assertEquals(it, result.method) }
      file?.also { assertEquals(it, result.file) }
      version?.also { assertEquals(it, result.version) }
    }
  }

  @Test
  fun testHttpNoPort() = runTest {
    val parser =
        UrlRequestParser(
            urlFixers = mutableSetOf(),
        )

    // http://example.com -> http example.com 80
    parser.testSuccess(
        line = "GET http://example.com HTTP/1.0",
        method = "GET",
        version = "HTTP/1.0",
        host = "example.com",
        port = 80,
        file = "/",
    )
  }

  @Test
  fun testHttpWeirdPort() = runTest {
    val parser =
        UrlRequestParser(
            urlFixers = mutableSetOf(),
        )

    // http://example.com:69 -> http example.com 69
    parser.testSuccess(
        line = "GET http://example.com:69 HTTP/1.0",
        method = "GET",
        version = "HTTP/1.0",
        host = "example.com",
        port = 69,
        file = "/",
    )
  }

  @Test
  fun testHttpDefaultPortNoSchema() = runTest {
    val parser =
        UrlRequestParser(
            urlFixers = mutableSetOf(),
        )

    // example.com -> http example.com 80
    parser.testSuccess(
        line = "GET example.com HTTP/1.0",
        method = "GET",
        version = "HTTP/1.0",
        host = "example.com",
        port = 80,
        file = "/",
    )
  }

  @Test
  fun testHttpsNoSchemaNoPort() = runTest {
    val parser =
        UrlRequestParser(
            urlFixers = mutableSetOf(),
        )

    // example.com:443 -> https example.com 443
    parser.testSuccess(
        line = "GET example.com:443 HTTP/1.0",
        method = "GET",
        version = "HTTP/1.0",
        host = "example.com",
        port = 443,
        file = "/",
    )
  }

  @Test
  fun testHttpsNoSchemaWithPort() = runTest {
    val parser =
        UrlRequestParser(
            urlFixers = mutableSetOf(),
        )

    // example.com:443 -> https example.com 443
    parser.testSuccess(
        line = "GET example.com:443 HTTP/1.0",
        method = "GET",
        version = "HTTP/1.0",
        host = "example.com",
        port = 443,
        file = "/",
    )
  }

  @Test
  fun testHttpsWithSchemaNoPort() = runTest {
    val parser =
        UrlRequestParser(
            urlFixers = mutableSetOf(),
        )

    // https://example.com -> https example.com 443
    parser.testSuccess(
        line = "GET https://example.com HTTP/1.0",
        method = "GET",
        version = "HTTP/1.0",
        host = "example.com",
        port = 443,
        file = "/",
    )
  }

  @Test
  fun testHttpsWithSchemaWeirdPort() = runTest {
    val parser =
        UrlRequestParser(
            urlFixers = mutableSetOf(),
        )

    // https://example.com:80 -> https example.com 69
    parser.testSuccess(
        line = "GET https://example.com:69 HTTP/1.0",
        method = "GET",
        version = "HTTP/1.0",
        host = "example.com",
        port = 69,
        file = "/",
    )
  }

  @Test
  fun testHttpsSchemaNoPortWithFile() = runTest {
    val parser =
        UrlRequestParser(
            urlFixers = mutableSetOf(),
        )
    // https://example.com/hello.html -> https example.com 443 /hello.html
    parser.testSuccess(
        line = "GET https://example.com/hello.html HTTP/1.0",
        method = "GET",
        version = "HTTP/1.0",
        host = "example.com",
        port = 443,
        file = "/hello.html",
    )
  }

  @Test
  fun testHttpNoSchemaNoPortWithFile() = runTest {
    val parser =
        UrlRequestParser(
            urlFixers = mutableSetOf(),
        )

    // example.com/hello.html -> http example.com 80 /hello.html
    parser.testSuccess(
        line = "GET example.com/hello.html HTTP/1.0",
        method = "GET",
        version = "HTTP/1.0",
        host = "example.com",
        port = 80,
        file = "/hello.html",
    )
  }

  @Test
  fun testHttpsNoSchemaWithPortWithFile() = runTest {
    val parser =
        UrlRequestParser(
            urlFixers = mutableSetOf(),
        )
    // example.com:443/hello.html -> https example.com 443 /hello.html
    parser.testSuccess(
        line = "GET example.com:443/hello.html HTTP/1.0",
        method = "GET",
        version = "HTTP/1.0",
        host = "example.com",
        port = 443,
        file = "/hello.html",
    )
  }

  @Test
  fun testHttpWithSchemaWeirdPortWithFile() = runTest {
    val parser =
        UrlRequestParser(
            urlFixers = mutableSetOf(),
        )
    // http://example.com:69/hello.html -> http example.com 69 /hello.html
    parser.testSuccess(
        line = "GET http://example.com:69/hello.html HTTP/1.0",
        method = "GET",
        version = "HTTP/1.0",
        host = "example.com",
        port = 69,
        file = "/hello.html",
    )
  }

  @Test
  fun testHttpWithSchemaWeirdPortWithFileWithParameters() = runTest {
    val parser =
        UrlRequestParser(
            urlFixers = mutableSetOf(),
        )

    // http://example.com:69/hello.html?also=1&accept=2&args=3 -> http example.com 69
    // /hello.html?also=1&accept=2&args=3
    parser.testSuccess(
        line = "GET http://example.com:69/hello.html?also=1&accept=2&args=3 HTTP/1.0",
        method = "GET",
        version = "HTTP/1.0",
        host = "example.com",
        port = 69,
        file = "/hello.html?also=1&accept=2&args=3",
    )
  }

  @Test
  fun testHttpWithSchemaWeirdPortWithFileWithParametersWithHash() = runTest {
    val parser =
        UrlRequestParser(
            urlFixers = mutableSetOf(),
        )

    // http://example.com:69/hello.html?also=1&accept=2&args=3 -> http example.com 69
    // /hello.html?also=1&accept=2&args=3
    parser.testSuccess(
        line = "GET http://example.com:69/hello.html?also=1&accept=2&args=3#hashtag HTTP/1.0",
        method = "GET",
        version = "HTTP/1.0",
        host = "example.com",
        port = 69,
        file = "/hello.html?also=1&accept=2&args=3#hashtag",
    )
  }
}
