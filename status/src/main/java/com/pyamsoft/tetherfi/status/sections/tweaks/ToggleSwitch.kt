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

package com.pyamsoft.tetherfi.status.sections.tweaks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.pydroid.ui.haptics.LocalHapticManager

@Composable
internal fun ToggleSwitch(
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
  val hapticManager = LocalHapticManager.current

  Column(
      modifier =
          modifier
              .clickable(enabled = isEditable) {
                if (checked) {
                  hapticManager?.toggleOff()
                } else {
                  hapticManager?.toggleOn()
                }
                onClick()
              }
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
              MaterialTheme.typography.bodyLarge.copy(
                  fontWeight = FontWeight.W700,
                  color = color.copy(alpha = highAlpha),
              ),
      )
    }

    Text(
        modifier = Modifier.fillMaxWidth(),
        text = description,
        style =
            MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
    )
  }
}
