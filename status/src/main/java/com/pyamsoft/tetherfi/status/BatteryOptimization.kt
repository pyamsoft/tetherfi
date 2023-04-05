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
internal fun BatteryOptimization(
    modifier: Modifier = Modifier,
    isEditable: Boolean,
    appName: String,
    isBatteryOptimizationDisabled: Boolean,
    onDisableBatteryOptimizations: () -> Unit,
) {
  CheckableCard(
      modifier = modifier,
      isEditable = isEditable,
      condition = isBatteryOptimizationDisabled,
      title = "Ignore Battery Optimizations",
      description =
          """This will allow $appName to run at maximum performance.
            |
            |This will significantly enhance your networking experience but may use more battery.
            |(recommended)"""
              .trimMargin(),
      onClick = onDisableBatteryOptimizations,
  )
}
