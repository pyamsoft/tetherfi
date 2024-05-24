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
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.copyTo
import io.ktor.utils.io.writeFully

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
  output.apply {
    writeFully(writeMessageAndAwaitMore(response))
    writeFully(SOCKET_EOL.encodeToByteArray())
    flush()
  }
}

@CheckResult
internal suspend fun talk(input: ByteReadChannel, output: ByteWriteChannel): Long {
  // Should be faster than parsing byte buffers raw
  // input.joinTo(output, closeOnEnd = true)

  // https://github.com/pyamsoft/tetherfi/issues/279
  //
  // We want to keep track of how many total bytes we've worked with
  return input.copyTo(output, Long.MAX_VALUE)
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
