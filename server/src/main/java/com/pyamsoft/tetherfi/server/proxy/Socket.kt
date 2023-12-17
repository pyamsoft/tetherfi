package com.pyamsoft.tetherfi.server.proxy

import com.pyamsoft.tetherfi.server.proxy.session.tagSocket
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.SocketBuilder
import io.ktor.network.sockets.aSocket
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

internal suspend inline fun <T> usingSocket(
    dispatcher: CoroutineDispatcher,
    block: (SocketBuilder) -> T
): T {
  // Tag sockets for Android O strict mode
  tagSocket()

  val manager = SelectorManager(dispatcher = dispatcher)
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
      //
      // Update: 12/17/2023 - use our own server dispatcher
      withContext(context = dispatcher) { manager.close() }
    }
  }
}
