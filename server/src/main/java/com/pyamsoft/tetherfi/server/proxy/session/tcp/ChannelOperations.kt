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

private val DEFAULT_POOL by lazy {
  @Suppress("DEPRECATION") io.ktor.utils.io.core.internal.ChunkBuffer.Pool
}

/**
 * A duplicate of [ByteReadChannel.copyToImpl] but with a callback fired AFTER the read but BEFORE
 * the write operation
 */
@CheckResult
internal suspend inline fun ByteReadChannel.copyToWithActionBeforeWrite(
    dst: ByteWriteChannel,
    limit: Long,
    onBeforeWrite: (Int) -> Unit,
): Long {
  val buffer = DEFAULT_POOL.borrow()
  val dstNeedsFlush = !dst.autoFlush

  try {
    var copied = 0L

    while (true) {
      val remaining = limit - copied
      if (remaining == 0L) break
      buffer.resetForWrite(minOf(buffer.capacity.toLong(), remaining).toInt())

      val size = readAvailable(buffer)
      if (size == -1) break

      // The one line we added
      onBeforeWrite(size)

      dst.writeFully(buffer)
      copied += size

      if (dstNeedsFlush && availableForRead == 0) {
        dst.flush()
      }
    }
    return copied
  } catch (t: Throwable) {
    dst.close(t)
    throw t
  } finally {
    buffer.release(DEFAULT_POOL)
  }
}
