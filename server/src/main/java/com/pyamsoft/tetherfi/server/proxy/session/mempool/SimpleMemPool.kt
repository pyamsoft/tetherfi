package com.pyamsoft.tetherfi.server.proxy.session.mempool

import androidx.annotation.CheckResult
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal abstract class SimpleMemPool<T : Any> protected constructor() : MemPool<T> {

  protected val mutex = Mutex()
  protected val freeStorage = mutableSetOf<T>()
  protected val usedStorage = mutableSetOf<T>()

  private suspend fun release(instance: T) =
      mutex.withLock {
        usedStorage.remove(instance)
        freeStorage.add(instance)
      }

  final override suspend fun use(block: suspend (buffer: T) -> Unit) {
    val instance = claim()
    try {
      block(instance)
    } finally {
      release(instance)
    }
  }

  @CheckResult protected abstract suspend fun claim(): T

  @CheckResult protected abstract fun make(): T
}
