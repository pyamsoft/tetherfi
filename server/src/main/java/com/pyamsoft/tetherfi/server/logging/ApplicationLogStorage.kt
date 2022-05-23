package com.pyamsoft.tetherfi.server.logging

import com.pyamsoft.tetherfi.core.LogEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class ApplicationLogStorage<T : LogEvent> internal constructor() : LogStorage<T> {

  private val mutex = Mutex()
  private val storage = mutableListOf<T>()
  private val latest = MutableStateFlow<T?>(null)

  override suspend fun onLogEvent(block: suspend (T) -> Unit) {
    mutex.withLock {
      for (event in storage) {
        block(event)
      }
    }

    latest.collect { event ->
      if (event != null) {
        block(event)
      }
    }
  }

  override suspend fun submit(event: T) {
    // Do not add to the storage if the event already exists
    val exists: Any?

    mutex.withLock {
      exists = storage.firstOrNull { it.id == event.id }
      if (exists == null) {
        storage.add(event)
      }
    }

    if (exists == null) {
      latest.value = event
    }
  }

  override suspend fun clear() {
    mutex.withLock { storage.clear() }
    latest.value = null
  }
}
