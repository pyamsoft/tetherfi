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
import com.pyamsoft.pydroid.util.ifNotCancellation
import com.pyamsoft.tetherfi.core.Timber
import com.pyamsoft.tetherfi.server.clients.TetherClient
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.writeFully
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay

/** KTOR default buffer size, so we do too */
private const val BUFFER_SIZE = 4096L

/** Bandwidth is measured per second */
private val BANDWIDTH_INTERVAL_NANOS = 1.seconds.inWholeNanoseconds

/**
 * Line ending for socket messages
 *
 * Messages are not sent over a socket UNTIL this character pair is reached.
 */
const val SOCKET_EOL = "\r\n"

/** Right now we speak 1.1, maybe 2.0 someday */
const val PROXY_HTTP_VERSION = "HTTP/1.1"

/** Static proxy events */
private enum class ProxyEvents(
    val code: Int,
    val message: String,
) {
  CONNECT(200, "Connection Established"),
  ERROR(502, "Bad Gateway"),
  BLOCKED(403, "Forbidden")
}

/** Write a message to the proxy for various events */
private suspend fun proxyEvent(output: ByteWriteChannel, code: ProxyEvents) {
  proxyResponse(output, "$PROXY_HTTP_VERSION ${code.code} ${code.message}")
}

/**
 * Respond to the client with a message string
 *
 * Properly line-ended with flushed output
 */
private suspend fun proxyResponse(output: ByteWriteChannel, response: String) {
  // Don't attempt the write if the channel is closed
  if (output.isClosedForWrite) {
    return
  }

  output.apply {
    writeFully(writeMessageAndAwaitMore(response))
    writeFully(SOCKET_EOL.encodeToByteArray())
    flush()
  }
}

@CheckResult
private suspend fun enforceBandwidthLimit(
    client: TetherClient,
    bandwidthLimit: Long,
    startTimeNanos: Long,
    read: Long
): Long {
  val bytesCopied = client.limiter.updateAndGet(read)

  // If we are over the limit, we need to wait before continuing
  if (bytesCopied > bandwidthLimit) {
    val nowNanos = System.nanoTime()

    // It has been more than 1 second, reset the limits
    val combined = startTimeNanos + BANDWIDTH_INTERVAL_NANOS
    if (combined >= nowNanos) {
      val delayNanos = combined - nowNanos
      if (delayNanos > 0) {
        val delayMillis = delayNanos.nanoseconds.inWholeMilliseconds
        Timber.w {
          "Bandwidth limit: limit=${bandwidthLimit} amount=${bytesCopied} delay=${delayMillis}ms"
        }
        delay(delayMillis)
      }
    }

    // Then reset counter
    client.limiter.reset()
    return nowNanos
  }

  return 0L
}

/* @CheckResult */
internal suspend inline fun talk(
    client: TetherClient,
    input: ByteReadChannel,
    output: ByteWriteChannel,
    onCopied: (Long) -> Unit,
): Long {
  // Should be faster than parsing byte buffers raw
  // input.joinTo(output, closeOnEnd = true)

  // https://github.com/pyamsoft/tetherfi/issues/279
  //
  // We want to keep track of how many total bytes we've worked with
  var total = 0L

  // Rate Limiting (inline for performance)
  val bandwidthLimit = client.bandwidthLimit?.bytes ?: 0L
  val mustEnforceBandwidthLimit = bandwidthLimit > 0

  var startTimeNanos = System.nanoTime()

  // If nothing is copied, we abandon immediately
  while (!output.isClosedForWrite) {
    val copied: Long =
        try {
          // Use a small buffer size to not overflow the device memory with a single large
          // transaction.
          input.copyToWithActionBeforeWrite(output, BUFFER_SIZE) { read ->
            // Rate Limiting
            if (mustEnforceBandwidthLimit) {
              val resetNewTimeNanos =
                  enforceBandwidthLimit(
                      client = client,
                      bandwidthLimit = bandwidthLimit,
                      startTimeNanos = startTimeNanos,
                      read = read,
                  )
              if (resetNewTimeNanos > 0) {
                startTimeNanos = resetNewTimeNanos
              }
            }
          }
        } catch (e: Throwable) {
          e.ifNotCancellation {
            Timber.e(e) { "Error during socket talk $client" }

            // Return 0 bytes to stop the talking, BUT
            // we want to still remember all the work we've done up until this point.
            0
          }
        }

    if (copied <= 0) {
      break
    }

    // Reporting
    total += copied
    onCopied(copied)
  }

  return total
}

/** Write a generic error back to the client socket because something has gone wrong */
internal suspend fun writeProxyError(output: ByteWriteChannel) {
  proxyEvent(output, ProxyEvents.ERROR)
}

/** Response for a client that is blocked */
internal suspend fun writeClientBlocked(output: ByteWriteChannel) {
  proxyEvent(output, ProxyEvents.BLOCKED)
}

/** A CONNECT call has successfully created an HTTP tunnel */
internal suspend fun writeConnectSuccess(output: ByteWriteChannel) {
  proxyEvent(output, ProxyEvents.CONNECT)
}

/**
 * Convert a message string into a byte array
 *
 * Correctly end the line with return and newline
 */
@CheckResult
internal fun writeMessageAndAwaitMore(message: String): ByteArray {
  val msg = if (message.endsWith(SOCKET_EOL)) message else "${message}$SOCKET_EOL"
  return msg.encodeToByteArray()
}
