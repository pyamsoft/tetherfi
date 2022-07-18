package com.pyamsoft.tetherfi.server.proxy.session.udp.tracker

import java.io.Closeable
import kotlinx.coroutines.DisposableHandle

internal interface KeyedObjectProducer<K : Any, V : Any> {

  suspend fun use(key: K, block: suspend (value: V) -> Unit)
}

internal interface ManagedKeyedObjectProducer<K : Any, V : Any> :
    KeyedObjectProducer<K, V>, Closeable, DisposableHandle
