package com.pyamsoft.widefi.server.proxy.session.mempool

import kotlinx.coroutines.sync.withLock

internal abstract class UnboundedMemPool<T : Any> protected constructor() : SimpleMemPool<T>() {

  override suspend fun claim(): T =
      mutex.withLock {
        // If we do not have any free storage, make a new buffer and add it to free storage
        if (freeStorage.size <= 0) {
          freeStorage.add(make())
        }

        // Claim something from the free storage and track it in the used storage
        val instance = freeStorage.first()
        freeStorage.remove(instance)
        usedStorage.add(instance)

        return@withLock instance
      }
}
