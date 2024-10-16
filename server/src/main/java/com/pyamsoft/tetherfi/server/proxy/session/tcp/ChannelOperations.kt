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
import io.ktor.utils.io.InternalAPI
import io.ktor.utils.io.close
import io.ktor.utils.io.core.remaining

/**
 * A duplicate of [ByteReadChannel.copyTo] but with a callback fired AFTER the read but BEFORE the
 * write operation
 */
@CheckResult
@OptIn(InternalAPI::class)
internal suspend inline fun ByteReadChannel.copyToWithActionBeforeWrite(
    channel: ByteWriteChannel,
    limit: Long,
    onBeforeWrite: (Long) -> Unit,
): Long {
  var remaining = limit
  try {
    while (!isClosedForRead && remaining > 0) {
      if (readBuffer.exhausted()) awaitContent()
      val count = minOf(remaining, readBuffer.remaining)

      // This line was AFTER readBuffer.readTo but we move it BEFORE
      remaining -= count

      // We add this single line
      onBeforeWrite(limit - remaining)

      // This line was BEFORE remaining -= count but we moved it AFTER
      readBuffer.readTo(channel.writeBuffer, count)
      channel.flush()
    }
  } catch (cause: Throwable) {
    cancel(cause)
    channel.close(cause)
    throw cause
  } finally {
    channel.flush()
  }

  return limit - remaining
}
