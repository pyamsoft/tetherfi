package com.pyamsoft.tetherfi.server.proxy.session.udp.tracker

import androidx.annotation.CheckResult
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

internal abstract class SimpleKeyedObjectProducer<K : Any, V : Any> protected constructor() :
    ManagedKeyedObjectProducer<K, V> {

  private var isAlive = true

  protected val mutex = Mutex()
  protected val pool = mutableMapOf<K, V>()

  private suspend fun release(key: K, value: V) {
    if (!isAlive) {
      Timber.w("KeyedObjectPool is dead, cannot release")
      return
    }

    mutex.withLock {
      // TODO release somehow?
      Timber.d("This is where I would release $key $value, if I could")
    }
  }

  final override suspend fun use(key: K, block: suspend (value: V) -> Unit) {
    if (!isAlive) {
      throw IllegalStateException("KeyedObjectPool is dead, cannot use buffer!")
    }

    val value = claim(key)
    try {
      block(value)
    } finally {
      release(key, value)
    }
  }

  final override fun close() {
    if (!isAlive) {
      Timber.w("KeyedObjectPool is already closed, cannot close again")
      return
    }

    pool.forEach { entry ->
      val v = entry.value
      if (v is DisposableHandle) {
        v.dispose()
      }
    }

    pool.clear()
  }

  final override fun dispose() {
    try {
      close()
    } catch (ignore: Throwable) {}
  }

  @CheckResult protected abstract suspend fun claim(key: K): V

  @CheckResult protected abstract fun make(key: K): V
}
