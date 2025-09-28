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
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

sealed interface ServerSocketTimeout {

  val timeoutDuration: Duration

  enum class Defaults(
      override val timeoutDuration: Duration,
  ) : ServerSocketTimeout {
    INFINITE(timeoutDuration = Duration.INFINITE),
    SUPERFAST(timeoutDuration = 3.seconds),
    FAST(timeoutDuration = 10.seconds),
    BALANCED(timeoutDuration = 30.seconds),
    COMPAT(timeoutDuration = 1.minutes),
    NICE(timeoutDuration = 5.minutes),
  }

  data class BoundTimeout(
      override val timeoutDuration: Duration,
  ) : ServerSocketTimeout

  companion object {

    @JvmStatic
    @CheckResult
    fun create(timeoutInSeconds: Long): ServerSocketTimeout {
      // This is serlialized in the prefs as -1 for infinite
      val duration = if (timeoutInSeconds <= 0) Duration.INFINITE else timeoutInSeconds.seconds
      return BoundTimeout(timeoutDuration = duration)
    }
  }
}
