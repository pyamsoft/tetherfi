package com.pyamsoft.tetherfi.server.proxy.session.udp.tracker

import androidx.annotation.CheckResult
import kotlinx.coroutines.sync.withLock

internal abstract class UnboundedKeyedObjectProducer<K : Any, V : Any> protected constructor() :
    SimpleKeyedObjectProducer<K, V>() {

  override suspend fun claim(key: K): V =
      mutex.withLock {
        val v: V
        val existing = pool[key]
        // The value has to exist and still be valid
        if (existing != null && isValid(existing)) {
          v = existing
        } else {
          // Make a new value
          v = make(key)
          pool[key] = v
        }

        return@withLock v
      }

  @CheckResult protected abstract fun isValid(value: V): Boolean
}
