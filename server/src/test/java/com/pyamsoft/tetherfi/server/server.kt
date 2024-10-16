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

package com.pyamsoft.tetherfi.server

import androidx.annotation.CheckResult
import com.pyamsoft.tetherfi.server.proxy.session.tcp.SOCKET_EOL
import com.pyamsoft.tetherfi.server.proxy.usingConnection
import com.pyamsoft.tetherfi.server.proxy.usingSocketBuilder
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.utils.io.pool.ByteBufferPool
import io.ktor.utils.io.readAvailable
import io.ktor.utils.io.writeStringUtf8
import kotlin.concurrent.thread
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withContext

val RESPONSE_TEXT =
    """
    <html>
        <head>
            <title>Example Domain</title>
            <meta charset="utf-8">
            <meta http-equiv="Content-type" content="text/html; charset=utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1">
        </head>
        <body>
            <div>
                <h1>
                    Example Domain
                </h1>
                <p>
                    This domain is for use in illustrative examples in documents. You may use this
                    domain in literature without prior coordination or asking for permission.
                </p>
                <p>
                    <a href="https://www.iana.org/domains/example">
                        More information...
                    </a>
                </p>
            </div>
        </body>
    </html>
                   """
        .trimIndent()

@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
@CheckResult
internal suspend inline fun setupServer(
    scope: CoroutineScope,
    testServer: Boolean = false,
    withServer: CoroutineScope.() -> Unit,
) {
  val ktor =
      embeddedServer(
          factory = Netty,
          port = SERVER_PORT,
          host = HOSTNAME,
          module = { routing { get("/") { call.respondText(RESPONSE_TEXT) } } },
      )

  val thread = thread {
    println("Start KTOR server: ${Thread.currentThread().name} $SERVER_REMOTE")
    ktor.start(wait = true)
  }

  // Wait a bit to start
  delay(2.seconds)

  if (testServer) {
    usingSocketBuilder(newSingleThreadContext("KTOR")) { b ->
      b.tcp().connect(SERVER_REMOTE).usingConnection(autoFlush = true) { read, write ->
        val get =
            """
              GET / HTTP/1.1${SOCKET_EOL}

              Host: ${HOSTNAME}:${SERVER_PORT}"""
                .trimIndent() + SOCKET_EOL

        write.writeStringUtf8(get)
        ByteBufferPool().use { pool ->
          val dst = pool.borrow()
          try {
            val amt = read.readAvailable(dst)
            val res = String(dst.array(), 0, amt, Charsets.UTF_8)
            println("RESP: \"$res\"")
          } finally {
            pool.recycle(dst)
          }
        }
      }
    }
  }

  println("Run test with live server")
  scope.withServer()

  println("Shut down KTOR server")
  ktor.stop()

  withContext(Dispatchers.IO) {
    println("Await KTOR thread join")
    thread.join()
    println("KTOR thread completed")
  }
}
