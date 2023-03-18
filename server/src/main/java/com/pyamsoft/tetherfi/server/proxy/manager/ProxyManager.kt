package com.pyamsoft.tetherfi.server.proxy.manager

import androidx.annotation.CheckResult
import com.pyamsoft.tetherfi.server.proxy.SharedProxy
import kotlin.coroutines.CoroutineContext

internal interface ProxyManager {

  suspend fun loop(
      context: CoroutineContext,
      port: Int,
  )

  interface Factory {

    @CheckResult fun create(type: SharedProxy.Type): ProxyManager
  }
}
