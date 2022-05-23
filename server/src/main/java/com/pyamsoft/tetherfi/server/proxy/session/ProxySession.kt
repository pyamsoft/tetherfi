package com.pyamsoft.tetherfi.server.proxy.session

import androidx.annotation.CheckResult

internal interface ProxySession<CS : Any> {

  suspend fun exchange(proxy: CS)

  interface Factory<CS : Any> {

    @CheckResult fun create(): ProxySession<CS>
  }
}
