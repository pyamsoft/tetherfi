package com.pyamsoft.tetherfi.server

import com.pyamsoft.pydroid.util.ifNotCancellation
import com.pyamsoft.tetherfi.server.proxy.session.tcp.SOCKET_EOL
import com.pyamsoft.tetherfi.server.proxy.usingConnection
import com.pyamsoft.tetherfi.server.proxy.usingSocketBuilder
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.utils.io.pool.ByteBufferPool
import io.ktor.utils.io.readAvailable
import io.ktor.utils.io.writeStringUtf8
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

private fun makeGetRequest(port: Int): String {
  return listOf(
          "GET http://${HOSTNAME}:$port HTTP/1.1",
          "",
          "",
      )
      .joinToString(SOCKET_EOL)
}

private val GET_EXPECT_RESPONSE =
    """HTTP/1.1 200 OK
Content-Length: 753
Content-Type: text/plain; charset=UTF-8

$RESPONSE_TEXT"""
        .trimIndent()

class ProxyPerformanceTest {

  private suspend fun CoroutineScope.testServerPerformance(
      nThreads: Int,
      jobCount: Int,
      serverPort: Int,
      proxyPort: Int,
  ) {
    val completed = MutableStateFlow(0)
    try {
      setupServer(
          this,
          serverPort = serverPort,
      ) {
        setupProxy(
            this,
            proxyPort = proxyPort,
            nThreads = nThreads,
            isLoggingEnabled = true,
        ) { dispatcher ->
          ByteBufferPool().use { pool ->
            val proxyRemote = InetSocketAddress(hostname = HOSTNAME, port = proxyPort)
            val request = makeGetRequest(port = serverPort)
            val jobs: MutableCollection<Deferred<Unit>> = mutableSetOf()
            for (i in 0 until jobCount) {
              val job =
                  async(context = Dispatchers.IO) {
                    usingSocketBuilder(dispatcher) { builder ->
                      try {
                        builder
                            .tcp()
                            .connect(
                                remoteAddress = proxyRemote,
                                configure = {
                                  reuseAddress = true
                                  // As of KTOR-3.0.0, this is not supported and crashes at
                                  // runtime
                                  // reusePort = true
                                },
                            )
                            .usingConnection(autoFlush = true) { read, write ->
                              println("Send request $i")
                              write.writeStringUtf8(request)

                              val dst = pool.borrow()
                              try {
                                val amt = read.readAvailable(dst)

                                val res =
                                    String(dst.array(), 0, amt, Charsets.UTF_8)
                                        // Correct all the CRLF newlines to normal newlines
                                        // This is just for test correctness.
                                        .replace(SOCKET_EOL, System.lineSeparator())

                                assertEquals(GET_EXPECT_RESPONSE, res)
                                println("Got response $i")
                                completed.update { it + 1 }
                              } finally {
                                pool.recycle(dst)
                              }
                            }
                      } catch (e: Throwable) {
                        e.ifNotCancellation {
                          println("Error connecting proxy: $proxyRemote")
                          e.printStackTrace()
                          throw e
                        }
                      }
                    }
                  }

              jobs.add(job)
            }

            jobs.awaitAll()
          }
        }
      }

      assertEquals(completed.value, jobCount)
    } finally {
      println("Completed jobs: ${completed.value}")
    }
  }

  /** We can open a bunch of sockets right? */
  @Test
  fun singleThreadedServerPerformanceTest(): Unit =
      runBlockingWithDelays(1.minutes) {
        testServerPerformance(
            nThreads = 1,
            jobCount = 20,
            serverPort = 6666,
            proxyPort = 9999,
        )
      }

  /** We can open a bunch of sockets right? */
  @Test
  fun multiThreadedServerPerformanceTest(): Unit =
      runBlockingWithDelays(1.minutes) {
        testServerPerformance(
            nThreads = Runtime.getRuntime().availableProcessors(),
            jobCount = 40,
            serverPort = 6667,
            proxyPort = 9998,
        )
      }
}
