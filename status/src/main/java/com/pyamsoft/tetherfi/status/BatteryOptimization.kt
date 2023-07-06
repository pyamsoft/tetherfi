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

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.tetherfi.ui.Label
import com.pyamsoft.tetherfi.ui.checkable.CheckableCard

internal fun LazyListScope.renderBatteryAndPerformance(
    itemModifier: Modifier = Modifier,
    isEditable: Boolean,
    appName: String,
    state: StatusViewState,

    // Battery optimization
    onDisableBatteryOptimizations: () -> Unit,

    // Wake lock
    onToggleKeepWakeLock: () -> Unit,
    onToggleKeepWifiLock: () -> Unit,
) {
  item(
      contentType = StatusScreenContentTypes.BATTERY_LABEL,
  ) {
    Label(
        modifier =
            itemModifier
                .padding(horizontal = MaterialTheme.keylines.content)
                .padding(top = MaterialTheme.keylines.content)
                .padding(bottom = MaterialTheme.keylines.baseline),
        text = "Battery and Performance",
    )
  }

  item(
      contentType = StatusScreenContentTypes.WAKELOCKS,
  ) {
    Wakelocks(
        modifier =
            itemModifier
                .padding(horizontal = MaterialTheme.keylines.content)
                .padding(bottom = MaterialTheme.keylines.content),
        isEditable = isEditable,
        appName = appName,
        state = state,
        onToggleKeepWakeLock = onToggleKeepWakeLock,
        onToggleKeepWifiLock = onToggleKeepWifiLock,
    )
  }

  item(
      contentType = StatusScreenContentTypes.BATTERY_OPTIMIZATION,
  ) {
    BatteryOptimization(
        modifier = itemModifier.padding(horizontal = MaterialTheme.keylines.content),
        isEditable = isEditable,
        appName = appName,
        state = state,
        onDisableBatteryOptimizations = onDisableBatteryOptimizations,
    )
  }
}

@Composable
private fun BatteryOptimization(
    modifier: Modifier = Modifier,
    isEditable: Boolean,
    appName: String,
    state: StatusViewState,
    onDisableBatteryOptimizations: () -> Unit,
) {
  val isBatteryOptimizationDisabled by state.isBatteryOptimizationsIgnored.collectAsState()

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
