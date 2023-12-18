package com.pyamsoft.tetherfi.server.proxy

import androidx.annotation.CheckResult
import kotlinx.coroutines.CoroutineDispatcher

interface ServerDispatcher {

  val isPrimaryUnbound: Boolean

  /** Dispatcher for Primary effects, usually very performant */
  val primary: CoroutineDispatcher

  /** Side effect dispatcher, allocated fewer threads */
  val sideEffect: CoroutineDispatcher

  interface Factory {

    @CheckResult suspend fun create(): ServerDispatcher
  }
}
