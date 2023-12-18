package com.pyamsoft.tetherfi.server

import androidx.annotation.CheckResult

sealed interface ServerPerformanceLimit {

  val coroutineLimit: Int

  enum class Defaults(
      val coroutineLimit: Int,
  ) {
    UNBOUND(coroutineLimit = 0),
    BOUND_N_CPU(coroutineLimit = Runtime.getRuntime().availableProcessors()),
    BOUND_2N_CPU(coroutineLimit = 2 * BOUND_N_CPU.coroutineLimit),
    BOUND_3N_CPU(coroutineLimit = 3 * BOUND_N_CPU.coroutineLimit),
    BOUND_4N_CPU(coroutineLimit = 4 * BOUND_N_CPU.coroutineLimit),
    BOUND_5N_CPU(coroutineLimit = 5 * BOUND_N_CPU.coroutineLimit)
  }

  data class BoundLimit(
      override val coroutineLimit: Int,
  ) : ServerPerformanceLimit

  companion object {

    @JvmStatic
    @CheckResult
    fun create(limit: Int): ServerPerformanceLimit {
      return BoundLimit(limit)
    }
  }
}
