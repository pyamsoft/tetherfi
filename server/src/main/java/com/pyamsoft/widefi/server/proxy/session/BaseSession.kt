package com.pyamsoft.widefi.server.proxy.session

import com.pyamsoft.widefi.server.proxy.SharedProxy
import timber.log.Timber

internal abstract class BaseSession<CS : Any>(
  protected val proxyType: SharedProxy.Type,
  private val proxyDebug: Boolean,
) {

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

  abstract suspend fun exchange(proxy: CS)
}
