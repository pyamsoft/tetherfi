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

package com.pyamsoft.tetherfi.server.proxy

import com.pyamsoft.pydroid.util.ifNotCancellation
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.SocketBuilder
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.util.cio.use
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.cancel
import kotlinx.coroutines.CoroutineDispatcher

/** Build a socket in scope for a selector manager */
internal inline fun <T> usingSocketBuilder(
    dispatcher: CoroutineDispatcher,
    block: (SocketBuilder) -> T
): T {
  SelectorManager(dispatcher = dispatcher).use { manager ->
    val rawSocket = aSocket(manager)
    return block(rawSocket)
  }
}

/**
 * Open a socket for reading and writing
 *
 * Close the socket after any kinds of errors, or at the end of operations
 */
internal inline fun <T> Socket.usingConnection(
    autoFlush: Boolean,
    block: (ByteReadChannel, ByteWriteChannel) -> T
): T {
  this.use { socket ->
    val reader = socket.openReadChannel()
    try {
      socket.openWriteChannel(autoFlush = autoFlush).use {
        val writer = this
        return block(reader, writer)
      }
    } catch (e: Throwable) {
      e.ifNotCancellation {
        reader.cancel(e)
        throw e
      }
    } finally {
      reader.cancel()
    }
  }
}
