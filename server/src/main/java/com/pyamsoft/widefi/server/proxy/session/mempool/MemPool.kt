package com.pyamsoft.widefi.server.proxy.session.mempool

internal interface MemPool<T : Any> {

  suspend fun use(block: suspend (instance: T) -> Unit)
}
