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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.pydroid.ui.haptics.LocalHapticManager
import com.pyamsoft.tetherfi.ui.Label
import com.pyamsoft.tetherfi.ui.checkable.CheckableCard

internal fun LazyListScope.renderBattery(
    itemModifier: Modifier = Modifier,
    isEditable: Boolean,
    appName: String,
    state: StatusViewState,

    // Battery optimization
    onDisableBatteryOptimizations: () -> Unit,
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
        text = "Operating Settings",
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
  val hapticManager = LocalHapticManager.current
  val isBatteryOptimizationDisabled by state.isBatteryOptimizationsIgnored.collectAsState()

  val canEdit =
      remember(isEditable, isBatteryOptimizationDisabled) {
        isEditable && !isBatteryOptimizationDisabled
      }

  CheckableCard(
      modifier = modifier,
      isEditable = canEdit,
      condition = isBatteryOptimizationDisabled,
      title = "Always Alive",
      description =
          """This will allow the $appName Hotspot to continue running even if the app is closed.
            |
            |This will significantly enhance your experience and network performance.
            |(recommended)"""
              .trimMargin(),
      onClick = {
        if (!isBatteryOptimizationDisabled) {
          hapticManager?.confirmButtonPress()
          onDisableBatteryOptimizations()
        }
      },
  )
}
