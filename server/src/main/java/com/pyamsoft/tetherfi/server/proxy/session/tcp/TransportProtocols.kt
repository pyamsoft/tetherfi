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
import io.ktor.utils.io.copyTo
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.delay

/** Buffers are 4MB in size */
private const val BUFFER_MAX_SIZE = 4_194_304L

/** Bandwidth is measured per second */
private const val BANDWIDTH_INTERVAL = 1000L

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
private fun decideBufferSize(limit: Long): Long {
  return if (limit > 0L) {
    if (limit > BUFFER_MAX_SIZE) {
      BUFFER_MAX_SIZE
    } else {
      limit
    }
  } else {
    BUFFER_MAX_SIZE
  }
}

@CheckResult
internal suspend inline fun talk(
    client: TetherClient,
    input: ByteReadChannel,
    output: ByteWriteChannel,
): Long {
  // Should be faster than parsing byte buffers raw
  // input.joinTo(output, closeOnEnd = true)

  // https://github.com/pyamsoft/tetherfi/issues/279
  //
  // We want to keep track of how many total bytes we've worked with
  var total = 0L

  // Rate Limiting (inline for performance)
  val transferLimit = client.transferLimit
  val bandwidthLimit = transferLimit?.bytes ?: 0L
  val enforceBandwidthLimit = bandwidthLimit > 0L
  var startTime = System.currentTimeMillis()
  var bytesCopied = 0L

  // The buffer size is either our default 4M amount,
  // or the bandwidth limit IFF the limit is smaller than the
  // default size
  val bufferSize = decideBufferSize(bandwidthLimit)

  // If nothing is copied, we abandon immediately
  while (!output.isClosedForWrite) {
    val copied: Long =
        try {
          // Use a small buffer size to not overflow the device memory with a single large
          // transaction.
          input.copyTo(output, bufferSize)
        } catch (e: Throwable) {
          e.ifNotCancellation {
            Timber.e(e) { "Error during HTTP talk" }

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

    // Rate Limiting
    if (enforceBandwidthLimit) {
      bytesCopied += copied

      // If we are over the limit, we need to wait before continuing
      if (bytesCopied > bandwidthLimit) {
        val now = System.currentTimeMillis()
        // It has been more than 1 second, reset the limits
        val combined = startTime + BANDWIDTH_INTERVAL
        if (combined >= now) {
          val amount = combined - now
          if (amount > 0) {
            Timber.d { "Delay connection from bandwidth limit: $transferLimit wait ${amount}ms" }
            delay(amount)
          }
        }

        // Then reset counter
        startTime = now
        bytesCopied = 0L
      }
    }
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
