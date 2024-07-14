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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.tetherfi.status.R
import com.pyamsoft.tetherfi.status.sections.tweaks.ToggleSwitch
import com.pyamsoft.tetherfi.ui.checkable.rememberCheckableColor
import com.pyamsoft.tetherfi.ui.textAlpha

private enum class RenderWakelocksContentTypes {
  WAKE_LOCKS,
}

internal fun LazyListScope.renderWakelocks(
    itemModifier: Modifier = Modifier,
    appName: String,
    isEditable: Boolean,
) {
  item(
      contentType = RenderWakelocksContentTypes.WAKE_LOCKS,
  ) {
    val cardColor by
        rememberCheckableColor(
            enabled = isEditable,
            label = "Wake Locks",
            condition = true,
            selectedColor = MaterialTheme.colorScheme.primary,
        )

    Card(
        modifier = itemModifier.padding(bottom = MaterialTheme.keylines.content),
        shape = MaterialTheme.shapes.medium,
        border =
            BorderStroke(
                width = 2.dp,
                color = MaterialTheme.colorScheme.primaryContainer,
            ),
    ) {
      Column(
          modifier = Modifier.padding(vertical = MaterialTheme.keylines.content),
      ) {
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .padding(horizontal = MaterialTheme.keylines.content)
                    .padding(bottom = MaterialTheme.keylines.content),
            verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
              modifier = Modifier.weight(1F),
              text = stringResource(R.string.perf_wake_lock_title),
              style =
                  MaterialTheme.typography.titleLarge.copy(
                      fontWeight = FontWeight.W700,
                      color = cardColor,
                  ),
          )

          TriStateCheckbox(
              modifier = Modifier.padding(start = MaterialTheme.keylines.content),
              enabled = isEditable,
              state = ToggleableState.On,
              onClick = null,
          )
        }

        Text(
            modifier =
                Modifier.fillMaxWidth()
                    .padding(horizontal = MaterialTheme.keylines.content)
                    .padding(bottom = MaterialTheme.keylines.content),
            text = stringResource(R.string.perf_wake_lock_description, appName),
            style =
                MaterialTheme.typography.bodyMedium.copy(
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = textAlpha(isEditable),
                        ),
                ),
        )

        val wifiLockColor by
            rememberCheckableColor(
                enabled = isEditable,
                label = "Wi-Fi Lock",
                condition = true,
                selectedColor = MaterialTheme.colorScheme.primary,
            )

        ToggleSwitch(
            modifier = Modifier.fillMaxWidth(),
            isEditable = isEditable,
            color = wifiLockColor,
            checked = true,
            title = stringResource(R.string.perf_wifi_lock_title),
            description = stringResource(R.string.perf_wifi_lock_description),
            onClick = {},
        )

        val wakeLockColor by
            rememberCheckableColor(
                enabled = isEditable,
                label = "CPU Lock",
                condition = true,
                selectedColor = MaterialTheme.colorScheme.primary,
            )

        ToggleSwitch(
            modifier = Modifier.fillMaxWidth(),
            isEditable = isEditable,
            color = wakeLockColor,
            checked = true,
            title = stringResource(R.string.perf_cpu_lock_title),
            description = stringResource(R.string.perf_cpu_lock_description),
            onClick = {},
        )
      }
    }
  }
}
