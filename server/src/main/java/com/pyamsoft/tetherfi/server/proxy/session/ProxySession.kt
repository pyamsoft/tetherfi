package com.pyamsoft.tetherfi.server.proxy.session

import androidx.annotation.CheckResult
import kotlin.coroutines.CoroutineContext

internal interface ProxySession<T : ProxyData> {

  suspend fun exchange(
      context: CoroutineContext,
      data: T,
  )

  interface Factory<T : ProxyData> {

    @CheckResult fun create(): ProxySession<T>
  }
}
