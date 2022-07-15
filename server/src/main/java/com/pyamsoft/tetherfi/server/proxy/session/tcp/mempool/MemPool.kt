package com.pyamsoft.tetherfi.server.proxy.session.tcp.mempool

import java.io.Closeable
import kotlinx.coroutines.DisposableHandle

internal interface MemPool<T : Any> {

  suspend fun use(block: suspend (instance: T) -> Unit)
}

internal interface ManagedMemPool<T : Any> : MemPool<T>, Closeable, DisposableHandle
