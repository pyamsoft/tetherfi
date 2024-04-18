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

package com.pyamsoft.tetherfi.status.sections.performance

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.pydroid.ui.util.bottomBorder
import com.pyamsoft.pydroid.ui.util.sideBorders
import com.pyamsoft.pydroid.ui.util.topBorder
import com.pyamsoft.tetherfi.status.StatusViewState
import com.pyamsoft.tetherfi.status.sections.tweaks.ToggleSwitch
import com.pyamsoft.tetherfi.ui.BetterSurface
import com.pyamsoft.tetherfi.ui.checkable.rememberCheckableColor

private enum class RenderWakelocksContentTypes {
  LABEL,
  EXPLAIN,
  WIFI_LOCK,
  WAKE_LOCK,
}

internal fun LazyListScope.renderWakelocks(
    itemModifier: Modifier = Modifier,
    appName: String,
    isEditable: Boolean,
    state: StatusViewState,
    onToggleKeepWakeLock: () -> Unit,
    onToggleKeepWifiLock: () -> Unit,
) {
  item(
      contentType = RenderWakelocksContentTypes.LABEL,
  ) {
    val keepWakeLock by state.keepWakeLock.collectAsStateWithLifecycle()
    val keepWifiLock by state.keepWifiLock.collectAsStateWithLifecycle()

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
            selectedColor = MaterialTheme.colorScheme.primary,
        )

    val highAlpha = 1F
    val mediumAlpha = 1F

    BetterSurface(
        modifier =
            Modifier.topBorder(
                strokeWidth = 2.dp,
                color =
                    MaterialTheme.colorScheme.primary.copy(
                        alpha = mediumAlpha,
                    ),
                cornerRadius = MaterialTheme.keylines.content,
            ),
        shape =
            MaterialTheme.shapes.medium.copy(
                bottomStart = ZeroCornerSize,
                bottomEnd = ZeroCornerSize,
            ),
    ) {
      Row(
          modifier = itemModifier.padding(MaterialTheme.keylines.content),
          verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
            modifier = Modifier.weight(1F),
            text = "Wake Locks",
            style =
                MaterialTheme.typography.titleLarge.copy(
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
    }
  }

  item(
      contentType = RenderWakelocksContentTypes.EXPLAIN,
  ) {
    val mediumAlpha = 1F

    BetterSurface(
        modifier =
            Modifier.sideBorders(
                strokeWidth = 2.dp,
                color =
                    MaterialTheme.colorScheme.primary.copy(
                        alpha = mediumAlpha,
                    ),
            ),
    ) {
      Text(
          modifier =
              itemModifier
                  .fillMaxWidth()
                  .padding(horizontal = MaterialTheme.keylines.content)
                  .padding(bottom = MaterialTheme.keylines.content * 2),
          text =
              """Wake Locks keep $appName performance fast even when the screen is off and the system is in a low power mode.
                  |
                  |Your device may need one or both of these options enabled for good network performance, but some devices do not. You may notice increased battery usage with these options enabled."""
                  .trimMargin(),
          style =
              MaterialTheme.typography.bodySmall.copy(
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
              ),
      )
    }
  }

  item(
      contentType = RenderWakelocksContentTypes.WIFI_LOCK,
  ) {
    val highAlpha = 1F
    val mediumAlpha = 1F

    val keepWifiLock by state.keepWifiLock.collectAsStateWithLifecycle()
    val wifiLockColor by
        rememberCheckableColor(
            label = "Wi-Fi Lock",
            condition = keepWifiLock,
            selectedColor = MaterialTheme.colorScheme.primary,
        )

    BetterSurface(
        modifier =
            Modifier.sideBorders(
                strokeWidth = 2.dp,
                color =
                    MaterialTheme.colorScheme.primary.copy(
                        alpha = mediumAlpha,
                    ),
            ),
    ) {
      ToggleSwitch(
          modifier = itemModifier,
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
    }
  }

  item(
      contentType = RenderWakelocksContentTypes.WAKE_LOCK,
  ) {
    val highAlpha = 1F
    val mediumAlpha = 1F

    val keepWakeLock by state.keepWakeLock.collectAsStateWithLifecycle()
    val wakeLockColor by
        rememberCheckableColor(
            label = "CPU Lock",
            condition = keepWakeLock,
            selectedColor = MaterialTheme.colorScheme.primary,
        )

    BetterSurface(
        modifier =
            Modifier.bottomBorder(
                strokeWidth = 2.dp,
                color =
                    MaterialTheme.colorScheme.primary.copy(
                        alpha = mediumAlpha,
                    ),
                cornerRadius = MaterialTheme.keylines.content,
            ),
        shape =
            MaterialTheme.shapes.medium.copy(
                topStart = ZeroCornerSize,
                topEnd = ZeroCornerSize,
            ),
    ) {
      ToggleSwitch(
          modifier = itemModifier,
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
