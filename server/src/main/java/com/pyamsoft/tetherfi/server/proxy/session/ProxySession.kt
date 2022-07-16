package com.pyamsoft.tetherfi.server.proxy.session

import androidx.annotation.CheckResult

internal interface ProxySession<T : Any> {

  suspend fun exchange(data: T)

  interface Factory<T : Any> {

    @CheckResult fun create(): ProxySession<T>
  }
}
