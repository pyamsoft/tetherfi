package com.pyamsoft.tetherfi.server.proxy.session

import com.pyamsoft.tetherfi.server.proxy.SharedProxy
import timber.log.Timber

internal abstract class BaseProxySession<T : ProxyData>
protected constructor(
    private val proxyType: SharedProxy.Type,
    private val proxyDebug: Boolean,
) : ProxySession<T> {

  /** Log only when session is in debug mode */
  protected inline fun debugLog(message: () -> String) {
    if (proxyDebug) {
      Timber.d("${proxyType.name}: ${message()}")
    }
  }

  /** Log only when session is in debug mode */
  protected inline fun warnLog(message: () -> String) {
    if (proxyDebug) {
      Timber.w("${proxyType.name}: ${message()}")
    }
  }

  /** Log only when session is in debug mode */
  protected inline fun errorLog(throwable: Throwable, message: () -> String) {
    if (proxyDebug) {
      Timber.e(throwable, "${proxyType.name}: ${message()}")
    }
  }
}
