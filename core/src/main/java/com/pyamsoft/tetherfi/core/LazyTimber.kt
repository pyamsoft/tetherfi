package com.pyamsoft.tetherfi.core

import timber.log.Timber as RealTimber

object Timber {

  inline fun d(message: () -> String) {
    if (RealTimber.treeCount > 0) {
      RealTimber.d(message())
    }
  }

  inline fun w(message: () -> String) {
    if (RealTimber.treeCount > 0) {
      RealTimber.w(message())
    }
  }

  inline fun e(throwable: Throwable, message: () -> String) {
    if (RealTimber.treeCount > 0) {
      RealTimber.e(throwable, message())
    }
  }
}
