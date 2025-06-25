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

package com.pyamsoft.tetherfi.server.proxy.session.tcp

import androidx.annotation.CheckResult
import com.pyamsoft.pydroid.util.ifNotCancellation
import com.pyamsoft.tetherfi.core.Timber
import com.pyamsoft.tetherfi.server.clients.TetherClient
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay

/**
 * Line ending for socket messages
 *
 * Messages are not sent over a socket UNTIL this character pair is reached.
 */
const val SOCKET_EOL = "\r\n"

/** Bandwidth is measured per second */
private val BANDWIDTH_INTERVAL_NANOS = 1.seconds.inWholeNanoseconds

@CheckResult
internal suspend fun enforceBandwidthLimit(
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
