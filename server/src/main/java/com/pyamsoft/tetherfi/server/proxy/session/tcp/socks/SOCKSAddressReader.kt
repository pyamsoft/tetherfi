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

package com.pyamsoft.tetherfi.server.proxy.session.tcp.socks

import androidx.annotation.CheckResult
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readByte
import kotlinx.io.bytestring.ByteStringBuilder
import kotlinx.io.bytestring.decodeToString

private const val NULL_BYTE: Byte = 0

@CheckResult
internal suspend fun ByteReadChannel.readUntilNullTerminator(): String {
  val builder = ByteStringBuilder()

  // TODO(Peter): This is very slow
  // We want to read up until a delimeter null byte and then return the string
  while (!isClosedForRead) {
    val maybe = readByte()
    if (maybe == NULL_BYTE) {
      break
    }
    builder.append(maybe)
  }

  return builder.toByteString().decodeToString()
}
