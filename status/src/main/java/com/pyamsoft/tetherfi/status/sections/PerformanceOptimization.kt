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

package com.pyamsoft.tetherfi.status.sections

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.Card
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.pydroid.ui.defaults.CardDefaults
import com.pyamsoft.tetherfi.status.StatusScreenContentTypes
import com.pyamsoft.tetherfi.status.StatusViewState
import com.pyamsoft.tetherfi.status.ToggleSwitch
import com.pyamsoft.tetherfi.ui.Label
import com.pyamsoft.tetherfi.ui.checkable.rememberCheckableColor

internal fun LazyListScope.renderPerformance(
    itemModifier: Modifier = Modifier,
    isEditable: Boolean,
    appName: String,
    state: StatusViewState,

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
                .padding(top = MaterialTheme.keylines.content)
                .padding(bottom = MaterialTheme.keylines.baseline),
        text = "Performance Settings",
    )
  }

  item(
      contentType = StatusScreenContentTypes.WAKELOCKS,
  ) {
    Wakelocks(
        modifier = itemModifier,
        isEditable = isEditable,
        appName = appName,
        state = state,
        onToggleKeepWakeLock = onToggleKeepWakeLock,
        onToggleKeepWifiLock = onToggleKeepWifiLock,
    )
  }
}

@Composable
private fun Wakelocks(
    modifier: Modifier = Modifier,
    appName: String,
    isEditable: Boolean,
    state: StatusViewState,
    onToggleKeepWakeLock: () -> Unit,
    onToggleKeepWifiLock: () -> Unit,
) {
  val keepWakeLock by state.keepWakeLock.collectAsStateWithLifecycle()
  val keepWifiLock by state.keepWifiLock.collectAsStateWithLifecycle()

  val wakeLockColor by
      rememberCheckableColor(
          label = "CPU Lock",
          condition = keepWakeLock,
          selectedColor = MaterialTheme.colors.primary,
      )
  val wifiLockColor by
      rememberCheckableColor(
          label = "Wi-Fi Lock",
          condition = keepWifiLock,
          selectedColor = MaterialTheme.colors.primary,
      )

  val checkboxState =
      remember(
          keepWakeLock,
          keepWifiLock,
      ) {
        if (!keepWakeLock && !keepWifiLock) {
          ToggleableState.Off
        } else if (keepWakeLock && keepWifiLock) {
          ToggleableState.On
        } else {
          ToggleableState.Indeterminate
        }
      }

  val isChecked = remember(checkboxState) { checkboxState != ToggleableState.Off }
  val cardColor by
      rememberCheckableColor(
          label = "Wake Locks",
          condition = isChecked,
          selectedColor = MaterialTheme.colors.primary,
      )
  val highAlpha = if (isEditable) ContentAlpha.high else ContentAlpha.disabled
  val mediumAlpha = if (isEditable) ContentAlpha.medium else ContentAlpha.disabled

  Card(
      modifier =
          modifier.border(
              width = 2.dp,
              color = cardColor.copy(alpha = mediumAlpha),
              shape = MaterialTheme.shapes.medium,
          ),
      elevation = CardDefaults.Elevation,
  ) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
      Row(
          modifier = Modifier.padding(MaterialTheme.keylines.content),
          verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
            modifier = Modifier.weight(1F),
            text = "Wake Locks",
            style =
                MaterialTheme.typography.h6.copy(
                    fontWeight = FontWeight.W700,
                    color = cardColor.copy(alpha = highAlpha),
                ),
        )

        TriStateCheckbox(
            modifier = Modifier.padding(start = MaterialTheme.keylines.content),
            enabled = isEditable,
            state = checkboxState,
            onClick = null,
        )
      }

      Text(
          modifier =
              Modifier.fillMaxWidth()
                  .padding(horizontal = MaterialTheme.keylines.content)
                  .padding(bottom = MaterialTheme.keylines.content * 2),
          text =
              """Wake Locks keep $appName performance fast even when the screen is off and the system is in a low power mode.
                  |
                  |Your device may need one or both of these options enabled for good network performance, but some devices do not. You may notice increased battery usage with these options enabled."""
                  .trimMargin(),
          style =
              MaterialTheme.typography.caption.copy(
                  color = MaterialTheme.colors.onSurface.copy(alpha = mediumAlpha),
              ),
      )

      ToggleSwitch(
          highAlpha = highAlpha,
          mediumAlpha = mediumAlpha,
          isEditable = isEditable,
          color = wifiLockColor,
          checked = keepWifiLock,
          title = "Keep WiFi Awake",
          description =
              "You should try this option first if Internet speed is slow on speed-tests while the screen is off.",
          onClick = onToggleKeepWifiLock,
      )

      ToggleSwitch(
          highAlpha = highAlpha,
          mediumAlpha = mediumAlpha,
          isEditable = isEditable,
          color = wakeLockColor,
          checked = keepWakeLock,
          title = "Keep CPU Awake",
          description =
              "If WiFi is kept awake, and Internet speed is still slow on tests, you may need this option.",
          onClick = onToggleKeepWakeLock,
      )
    }
  }
}
