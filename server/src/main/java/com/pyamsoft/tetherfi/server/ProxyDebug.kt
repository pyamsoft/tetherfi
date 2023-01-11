package com.pyamsoft.tetherfi.server

import androidx.annotation.CheckResult
import com.pyamsoft.tetherfi.server.proxy.SharedProxy

internal enum class ProxyDebug {

  /**
   * No debug messages
   */
  NONE,

  /**
   * TCP related messages
   */
  TCP,

  /**
   * UDP related messages
   */
  UDP,

  /**
   * All debug messages
   */
  ALL;

  /**
   * We have to be allowed to debug this type
   */
  @CheckResult
  fun isAllowed(type: SharedProxy.Type): Boolean {
    if (this == NONE) {
      return false
    }

    if (this == ALL) {
      return true
    }

    return when (type) {
      SharedProxy.Type.TCP -> this == TCP
      SharedProxy.Type.UDP -> this == UDP
    }
  }
}
