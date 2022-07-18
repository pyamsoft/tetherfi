package com.pyamsoft.tetherfi.server.proxy.session

import androidx.annotation.CheckResult
import com.pyamsoft.tetherfi.server.proxy.SharedProxy
import timber.log.Timber

internal abstract class BaseProxySession<CS : Any>
protected constructor(
    private val proxyType: SharedProxy.Type,
    private val urlFixers: Set<UrlFixer>,
    private val proxyDebug: Boolean,
) : ProxySession<CS> {

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

  /**
   * Some connection request formats are buggy, this method seeks to fix them to what it knows in
   * very specific cases is correct
   */
  @CheckResult
  protected fun String.fixSpecialBuggyUrls(): String {
    var result = this
    for (fixer in urlFixers) {
      result = fixer.fix(result)
    }
    return result
  }
}
