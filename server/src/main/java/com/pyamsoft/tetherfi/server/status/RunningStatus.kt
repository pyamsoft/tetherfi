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

package com.pyamsoft.tetherfi.server.status

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

@Stable
@Immutable
sealed interface RunningStatus {
  data object NotRunning : RunningStatus

  data object Starting : RunningStatus

  data object Running : RunningStatus

  data object Stopping : RunningStatus

  data class HotspotError(
      override val throwable: Throwable,
  ) : Error(throwable)

  data class ProxyError(
      override val throwable: Throwable,
  ) : Error(throwable)

  abstract class Error
  protected constructor(
      open val throwable: Throwable,
  ) : RunningStatus
}
