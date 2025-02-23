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

package com.pyamsoft.tetherfi.server.proxy.session.tcp.socks.five

import androidx.annotation.CheckResult
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readByte
import io.ktor.utils.io.readPacket
import io.ktor.utils.io.readShort
import kotlinx.io.Source
import kotlinx.io.bytestring.ByteString
import kotlinx.io.readByteArray
import kotlinx.io.readByteString

internal sealed interface SourceOrByteReadChannel {

  @CheckResult suspend fun readByte(): Byte

  @CheckResult suspend fun readShort(): Short

  @CheckResult suspend fun readByteArray(byteCount: Int): ByteArray

  @CheckResult suspend fun readByteString(byteCount: Int): ByteString

  @JvmInline
  value class FromSource(
      private val source: Source,
  ) : SourceOrByteReadChannel {

    override suspend fun readByte(): Byte {
      return source.readByte()
    }

    override suspend fun readShort(): Short {
      return source.readShort()
    }

    override suspend fun readByteArray(byteCount: Int): ByteArray {
      return source.readByteArray(byteCount)
    }

    override suspend fun readByteString(byteCount: Int): ByteString {
      return source.readByteString(byteCount)
    }
  }

  @JvmInline
  value class FromByteReadChannel(
      private val channel: ByteReadChannel,
  ) : SourceOrByteReadChannel {

    override suspend fun readByte(): Byte {
      return channel.readByte()
    }

    override suspend fun readShort(): Short {
      return channel.readShort()
    }

    override suspend fun readByteArray(byteCount: Int): ByteArray {
      return channel.readPacket(byteCount).readByteArray()
    }

    override suspend fun readByteString(byteCount: Int): ByteString {
      return channel.readPacket(byteCount).readByteString()
    }
  }
}
