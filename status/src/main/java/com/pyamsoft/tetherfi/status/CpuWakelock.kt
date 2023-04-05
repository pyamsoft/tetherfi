/*
 * Copyright 2023 pyamsoft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pyamsoft.tetherfi.status

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pyamsoft.tetherfi.ui.checkable.CheckableCard

@Composable
internal fun CpuWakelock(
    modifier: Modifier = Modifier,
    appName: String,
    isEditable: Boolean,
    keepWakeLock: Boolean,
    onToggleKeepWakeLock: () -> Unit,
) {
  CheckableCard(
      modifier = modifier,
      isEditable = isEditable,
      condition = keepWakeLock,
      title = "Keep CPU Awake",
      description =
          """This will significantly improve $appName when the screen is off. Without it, you may notice extreme network slow down.
            |
            |This will use more battery, as it prevents your device from entering a deep-sleep state.
            |(recommended)"""
              .trimMargin(),
      onClick = onToggleKeepWakeLock,
  )
}
