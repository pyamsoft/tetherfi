package com.pyamsoft.tetherfi.server.proxy.connector

import androidx.annotation.CheckResult
import com.pyamsoft.tetherfi.server.proxy.SharedProxy

internal interface ProxyManager {

  suspend fun loop()

  interface Factory {

    @CheckResult
    fun create(
        type: SharedProxy.Type,
        port: Int,
    ): ProxyManager
  }
}
