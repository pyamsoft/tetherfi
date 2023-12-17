package com.pyamsoft.tetherfi.server

import androidx.annotation.CheckResult

sealed interface ServerPerformanceLimit {

  val coroutineLimit: Int

  enum class Defaults(
      val coroutineLimit: Int,
  ) {
    UNBOUND(0),
    BOUND_12(12),
    BOUND_24(24),
    BOUND_48(48),
    BOUND_64(64),
    BOUND_100(100),
  }

  data object LimitUnbound : ServerPerformanceLimit {
    override val coroutineLimit = 0
  }

  data class BoundLimit(
      override val coroutineLimit: Int,
  ) : ServerPerformanceLimit

  companion object {

    @JvmStatic
    @CheckResult
    fun create(limit: Int): ServerPerformanceLimit {
      if (limit <= 0) {
        return LimitUnbound
      }
      return BoundLimit(limit)
    }
  }
}
