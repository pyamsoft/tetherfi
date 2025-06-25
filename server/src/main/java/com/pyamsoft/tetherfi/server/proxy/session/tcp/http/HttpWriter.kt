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

package com.pyamsoft.tetherfi.server.proxy.session.tcp.http

import androidx.annotation.CheckResult
import com.pyamsoft.tetherfi.server.proxy.session.tcp.SOCKET_EOL
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.writeFully

/** Right now we speak 1.1, maybe 2.0 someday */
const val PROXY_HTTP_VERSION = "HTTP/1.1"

/** Write a generic error back to the client socket because something has gone wrong */
internal suspend fun writeProxyHttpError(output: ByteWriteChannel) {
  proxyEvent(output, ProxyHttpEvents.ERROR)
}

/** Response for a client that is blocked */
internal suspend fun writeHttpClientBlocked(output: ByteWriteChannel) {
  proxyEvent(output, ProxyHttpEvents.BLOCKED)
}

/** A CONNECT call has successfully created an HTTP tunnel */
internal suspend fun writeHttpConnectSuccess(output: ByteWriteChannel) {
  proxyEvent(output, ProxyHttpEvents.CONNECT)
}

/**
 * Convert a message string into a byte array
 *
 * Correctly end the line with return and newline
 */
@CheckResult
internal fun writeHttpMessageAndAwaitMore(message: String): ByteArray {
  val msg = if (message.endsWith(SOCKET_EOL)) message else "${message}$SOCKET_EOL"
  return msg.encodeToByteArray()
}

/** Static proxy events */
private enum class ProxyHttpEvents(
    val code: Int,
    val message: String,
) {
  CONNECT(200, "Connection Established"),
  ERROR(502, "Bad Gateway"),
  BLOCKED(403, "Forbidden")
}

/** Write a message to the proxy for various events */
private suspend fun proxyEvent(output: ByteWriteChannel, code: ProxyHttpEvents) {
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
    writeFully(writeHttpMessageAndAwaitMore(response))
    writeFully(SOCKET_EOL.encodeToByteArray())
    flush()
  }
}
