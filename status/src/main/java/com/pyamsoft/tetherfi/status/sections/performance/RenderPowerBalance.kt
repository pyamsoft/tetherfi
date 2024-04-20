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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.tetherfi.status.StatusViewState
import com.pyamsoft.tetherfi.ui.checkable.rememberCheckableColor

private enum class RenderThreadsContentTypes {
  POWER_BALANCE,
}

internal fun LazyListScope.renderPowerBalance(
    itemModifier: Modifier = Modifier,
    appName: String,
    isEditable: Boolean,
    state: StatusViewState,
    onShowPowerBalance: () -> Unit,
) {
  item(
      contentType = RenderThreadsContentTypes.POWER_BALANCE,
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

    Card(
        modifier = itemModifier.padding(bottom = MaterialTheme.keylines.content),
        border =
            BorderStroke(
                width = 2.dp,
                color = MaterialTheme.colorScheme.primaryContainer,
            ),
        shape = MaterialTheme.shapes.medium,
    ) {
      Column(modifier = Modifier.padding(MaterialTheme.keylines.content)) {
        Text(
            text = "Expert: Power Balance",
            style =
                MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.W700,
                    color = cardColor,
                ),
        )
        Text(
            modifier = Modifier.padding(bottom = MaterialTheme.keylines.content),
            text =
                """This is for Advanced Users.
                  |
                  |You can adjust the performance and resource usage of the $appName Hotspot, which will adjust the number of virtual threads allocated to the Hotspot Network.
                  |
                  |More Threads will result in usually faster performance, but higher battery usage and may heat up or slow down your device.
              """
                    .trimMargin(),
            style =
                MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
        )

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onShowPowerBalance,
            enabled = isEditable,
        ) {
          Text(
              text = "Adjust Power Balance",
          )
        }
      }
    }
  }
}
