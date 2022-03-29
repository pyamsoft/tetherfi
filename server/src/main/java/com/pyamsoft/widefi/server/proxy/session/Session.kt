package com.pyamsoft.widefi.server.proxy.session

import androidx.annotation.CheckResult
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.writeFully

/**
 * Respond to the client with a message string
 *
 * Properly line-ended with flushed output
 */
internal suspend fun proxyResponse(output: ByteWriteChannel, response: String) {
  output.apply {
    writeFully(writeMessageAndAwaitMore(response))
    writeFully(LINE_ENDING.encodeToByteArray())
    flush()
  }
}

private const val LINE_ENDING = "\r\n"

/**
 * Convert a message string into a byte array
 *
 * Correctly end the line with return and newline
 */
@CheckResult
internal fun writeMessageAndAwaitMore(message: String): ByteArray {
  val msg = if (message.endsWith(LINE_ENDING)) message else "${message}${LINE_ENDING}"
  return msg.encodeToByteArray()
}
