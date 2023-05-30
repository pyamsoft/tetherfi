package com.pyamsoft.tetherfi.service.lock

import androidx.annotation.CheckResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

internal abstract class AbstractLocker protected constructor() : Locker {

  protected abstract suspend fun acquireLock()

  protected abstract suspend fun releaseLock()

  @CheckResult protected abstract suspend fun isEnabled(): Boolean

  final override suspend fun acquire() =
      withContext(context = NonCancellable) {
        withContext(context = Dispatchers.Default) {
          releaseLock()

          if (isEnabled()) {
            acquireLock()
          }
        }
      }

  final override suspend fun release() =
      withContext(context = NonCancellable) {
        withContext(context = Dispatchers.Default) { releaseLock() }
      }
}
