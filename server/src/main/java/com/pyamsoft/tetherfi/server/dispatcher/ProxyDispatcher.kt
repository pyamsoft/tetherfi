package com.pyamsoft.tetherfi.server.dispatcher

import androidx.annotation.CheckResult
import kotlinx.coroutines.CoroutineDispatcher

interface ProxyDispatcher {

  @CheckResult fun ensureActiveDispatcher(): CoroutineDispatcher

  fun shutdown()
}
