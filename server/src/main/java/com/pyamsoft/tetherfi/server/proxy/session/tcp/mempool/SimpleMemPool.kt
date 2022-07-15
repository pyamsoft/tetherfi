package com.pyamsoft.tetherfi.server.proxy.session.tcp.mempool

import androidx.annotation.CheckResult
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

internal abstract class SimpleMemPool<T : Any> protected constructor() : ManagedMemPool<T> {

  private var isAlive = true

  protected val mutex = Mutex()
  protected val freeStorage = mutableSetOf<T>()
  protected val usedStorage = mutableSetOf<T>()

  private suspend fun release(instance: T) {
    if (!isAlive) {
      Timber.w("MemPool is dead, cannot release buffer")
      return
    }

    mutex.withLock {
      usedStorage.remove(instance)
      freeStorage.add(instance)
    }
  }

  final override suspend fun use(block: suspend (buffer: T) -> Unit) {
    if (!isAlive) {
      throw IllegalStateException("MemPool is dead, cannot use buffer!")
    }

    val instance = claim()
    try {
      block(instance)
    } finally {
      release(instance)
    }
  }

  override fun dispose() {
    try {
      close()
    } catch (ignore: Throwable) {}
  }

  override fun close() {
    if (!isAlive) {
      throw IllegalStateException("MemPool already closed, cannot close again")
    }

    Timber.d("Closing MemPool instance")
    isAlive = false

    // Release resources without using Mutex. This is a race so you should be sure that all work is
    // done before calling this method
    usedStorage.clear()
    freeStorage.clear()
  }

  @CheckResult protected abstract suspend fun claim(): T

  @CheckResult protected abstract fun make(): T
}
