/*
 * Copyright 2025 pyamsoft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pyamsoft.tetherfi.server

import androidx.annotation.CheckResult

sealed interface ServerPerformanceLimit {

  val coroutineLimit: Int

  enum class Defaults(
      override val coroutineLimit: Int,
  ) : ServerPerformanceLimit {
    UNBOUND(coroutineLimit = 0),
    BOUND_N_CPU(coroutineLimit = Runtime.getRuntime().availableProcessors()),
    BOUND_2N_CPU(coroutineLimit = 2 * BOUND_N_CPU.coroutineLimit),
    BOUND_3N_CPU(coroutineLimit = 3 * BOUND_N_CPU.coroutineLimit),
    BOUND_4N_CPU(coroutineLimit = 4 * BOUND_N_CPU.coroutineLimit),
    BOUND_5N_CPU(coroutineLimit = 5 * BOUND_N_CPU.coroutineLimit),
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
