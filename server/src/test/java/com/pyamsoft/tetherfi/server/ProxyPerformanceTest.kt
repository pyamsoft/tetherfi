package com.pyamsoft.tetherfi.server

import com.pyamsoft.pydroid.util.ifNotCancellation
import com.pyamsoft.tetherfi.server.proxy.session.tcp.SOCKET_EOL
import com.pyamsoft.tetherfi.server.proxy.usingConnection
import com.pyamsoft.tetherfi.server.proxy.usingSocketBuilder
import io.ktor.utils.io.pool.ByteBufferPool
import io.ktor.utils.io.readAvailable
import io.ktor.utils.io.writeStringUtf8
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

private val GET_REQUEST =
    listOf(
            "GET http://${HOSTNAME}:$SERVER_PORT HTTP/1.1",
            "",
            "",
        )
        .joinToString(SOCKET_EOL)

private val GET_EXPECT_RESPONSE =
    """HTTP/1.1 200 OK
Content-Length: 753
Content-Type: text/plain; charset=UTF-8

$RESPONSE_TEXT"""
        .trimIndent()

class ProxyPerformanceTest {

  /**
   * This is like runTest, but it does not skip delay() calls.
   *
   * We need to actually be able to delay, since server spinup takes a "little bit" of time.
   */
  private fun runBlockingWithDelays(
      timeout: Duration = 10.seconds,
      block: suspend CoroutineScope.() -> Unit,
  ): Unit = runBlocking {
    try {
      withTimeout(timeout, block)
    } catch (e: Throwable) {
      e.printStackTrace()
      throw e
    }
  }

  /** We can open a bunch of sockets right? */
  @Test
  fun serverPerformanceTest(): Unit =
      runBlockingWithDelays(1.minutes) {
        val completed = MutableStateFlow(0)
        try {
          setupServer(this) {
            setupProxy(this, isLoggingEnabled = true) { dispatcher ->
              ByteBufferPool().use { pool ->
                val jobs: MutableCollection<Deferred<Unit>> = mutableSetOf()
                for (i in 0 until 20) {
                  val job =
                      async(context = Dispatchers.IO) {
                        usingSocketBuilder(dispatcher) { builder ->
                          try {
                            builder
                                .tcp()
                                .connect(
                                    remoteAddress = PROXY_REMOTE,
                                    configure = {
                                      reuseAddress = true
                                      // As of KTOR-3.0.0, this is not supported and crashes at
                                      // runtime
                                      // reusePort = true
                                    },
                                )
                                .usingConnection(autoFlush = true) { read, write ->
                                  println("Send request $i")
                                  write.writeStringUtf8(GET_REQUEST)

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
                              println("Error connecting proxy: $PROXY_REMOTE")
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
        } finally {
          println("Completed jobs: ${completed.value}")
        }
      }

  /** We also are prepared to handle when a socket fails to open right? */
  @Test
  fun yoloFailThrows(): Unit = runBlockingWithDelays {
    setupProxy(
        this,
        expectServerFail = true,
        appEnv = { updateYolo(true) },
    ) {
      delay(5.seconds)
    }
  }
}
