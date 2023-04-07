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

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.pydroid.ui.defaults.CardDefaults
import com.pyamsoft.tetherfi.ui.checkable.rememberCheckableColor

@Composable
internal fun Wakelocks(
    modifier: Modifier = Modifier,
    appName: String,
    isEditable: Boolean,
    state: StatusViewState,
    onToggleKeepWakeLock: () -> Unit,
    onToggleKeepWifiLock: () -> Unit,
) {
  val keepWakeLock by state.keepWakeLock.collectAsState()
  val keepWifiLock by state.keepWifiLock.collectAsState()

  val wakeLockColor by rememberCheckableColor(keepWakeLock, MaterialTheme.colors.primary)
  val wifiLockColor by rememberCheckableColor(keepWifiLock, MaterialTheme.colors.primary)

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
  val cardColor by rememberCheckableColor(isChecked, MaterialTheme.colors.primary)
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

      WakelockSwitch(
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

      WakelockSwitch(
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

@Composable
private fun WakelockSwitch(
    modifier: Modifier = Modifier,
    isEditable: Boolean,
    highAlpha: Float,
    mediumAlpha: Float,
    color: Color,
    checked: Boolean,
    title: String,
    description: String,
    onClick: () -> Unit,
) {
  Column(
      modifier =
          modifier
              .clickable(enabled = isEditable) { onClick() }
              .padding(horizontal = MaterialTheme.keylines.content)
              .padding(bottom = MaterialTheme.keylines.content),
  ) {
    Row(
        modifier = Modifier.padding(bottom = MaterialTheme.keylines.baseline),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      Switch(
          modifier = Modifier.padding(end = MaterialTheme.keylines.content),
          enabled = isEditable,
          checked = checked,
          onCheckedChange = null,
      )

      Text(
          modifier = Modifier.weight(1F),
          text = title,
          style =
              MaterialTheme.typography.body1.copy(
                  fontWeight = FontWeight.W700,
                  color = color.copy(alpha = highAlpha),
              ),
      )
    }

    Text(
        modifier = Modifier.fillMaxWidth(),
        text = description,
        style =
            MaterialTheme.typography.caption.copy(
                color = MaterialTheme.colors.onSurface.copy(alpha = mediumAlpha),
            ),
    )
  }
}
