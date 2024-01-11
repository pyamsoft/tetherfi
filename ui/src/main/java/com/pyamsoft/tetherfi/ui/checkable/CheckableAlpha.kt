/*
 * Copyright 2024 pyamsoft
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

package com.pyamsoft.tetherfi.ui.checkable

import androidx.annotation.CheckResult
import androidx.compose.material.ContentAlpha
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

internal data class CheckableCardAlpha
internal constructor(
    val primary: Float,
    val secondary: Float,
)

@Composable
@CheckResult
internal fun rememberCheckableAlpha(isEditable: Boolean): CheckableCardAlpha {
  val highAlpha = ContentAlpha.high
  val mediumAlpha = ContentAlpha.medium
  val disabledAlpha = ContentAlpha.disabled

  return remember(
      isEditable,
      highAlpha,
      mediumAlpha,
      disabledAlpha,
  ) {
    val primary = if (isEditable) highAlpha else disabledAlpha
    val secondary = if (isEditable) mediumAlpha else disabledAlpha

    return@remember CheckableCardAlpha(primary, secondary)
  }
}
