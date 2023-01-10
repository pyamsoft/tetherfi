package com.pyamsoft.tetherfi.server.proxy.session

import androidx.annotation.CheckResult

internal interface ProxySession<T : ProxyData<*, *>> {

  suspend fun exchange(data: T)

  interface Factory<T : ProxyData<*, *>> {

    @CheckResult fun create(): ProxySession<T>
  }
}
