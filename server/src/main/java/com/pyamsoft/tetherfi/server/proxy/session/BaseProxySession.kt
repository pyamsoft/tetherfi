package com.pyamsoft.tetherfi.server.proxy.session

import com.pyamsoft.tetherfi.server.proxy.SharedProxy
import timber.log.Timber

internal abstract class BaseProxySession<CS : Any>
protected constructor(
    protected val proxyType: SharedProxy.Type,
    private val proxyDebug: Boolean,
) : ProxySession<CS> {

  /** Log only when session is in debug mode */
  protected inline fun debugLog(message: () -> String) {
    if (proxyDebug) {
      Timber.d(message())
    }
  }

  /** Log only when session is in debug mode */
  protected inline fun warnLog(message: () -> String) {
    if (proxyDebug) {
      Timber.w(message())
    }
  }
}
