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

/** KTOR default buffer size, so we do too */
const val BUFFER_SIZE = 4096L

/**
 * A duplicate of [ByteReadChannel.copyTo] but with a callback fired AFTER the read but BEFORE the
 * write operation
 */
@CheckResult
@OptIn(InternalAPI::class)
internal suspend inline fun ByteReadChannel.copyToWithActionBeforeWrite(
    dst: ByteWriteChannel,
    limit: Long,
    onBeforeWrite: (Long) -> Unit,
): Long {
  var remaining = limit
  try {
    while (!isClosedForRead && remaining > 0) {
      if (readBuffer.exhausted()) awaitContent()
      val count = minOf(remaining, readBuffer.remaining)

      // TODO(Peter): This is not very exact or reliable
      //
      // Because of how fast and optimized this readTo function is,
      // we don't really have a way to read->wait->write, so instead we
      // wait->readwrite
      //
      // We could instead implement our own read->wait->write using a bytearraypool
      // and reading to array buffers and then waiting, but this only gives around 75% of
      // the total performance, so we'd rather be fast at the cost of exact bandwidth correctness.
      onBeforeWrite(count)

      readBuffer.readTo(dst.writeBuffer, count)
      remaining -= count
      dst.flush()
    }
  } catch (cause: Throwable) {
    cancel(cause)
    dst.close(cause)
    throw cause
  } finally {
    dst.flush()
  }

  return limit - remaining
}
