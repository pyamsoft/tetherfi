package com.pyamsoft.tetherfi.server.proxy

import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.SocketBuilder
import io.ktor.network.sockets.aSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

internal suspend inline fun <T> usingSocket(block: (SocketBuilder) -> T): T {
  val manager = SelectorManager(dispatcher = Dispatchers.IO)
  val rawSocket = aSocket(manager)
  try {
    return block(rawSocket)
  } finally {
    withContext(context = NonCancellable) {
      // We use Dispatchers.IO because manager.close() could potentially block
      // which, if we used Dispatchers.Default could starve the thread.
      //
      // By using Dispatchers.IO we ensure this block runs on its own pooled thread
      // instead, so even if this blocks it will not resource starve others.
      withContext(context = Dispatchers.IO) { manager.close() }
    }
  }
}
