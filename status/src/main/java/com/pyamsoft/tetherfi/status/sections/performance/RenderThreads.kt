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

package com.pyamsoft.tetherfi.status.sections.performance

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material.Button
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.pydroid.ui.defaults.CardDefaults
import com.pyamsoft.pydroid.ui.util.bottomBorder
import com.pyamsoft.pydroid.ui.util.sideBorders
import com.pyamsoft.pydroid.ui.util.topBorder
import com.pyamsoft.tetherfi.status.StatusViewState
import com.pyamsoft.tetherfi.ui.BetterSurface
import com.pyamsoft.tetherfi.ui.checkable.rememberCheckableColor

private enum class RenderThreadsContentTypes {
  LABEL,
  EXPLAIN,
  ADJUST,
}

internal fun LazyListScope.renderThreads(
    itemModifier: Modifier = Modifier,
    appName: String,
    isEditable: Boolean,
    state: StatusViewState,
) {
  item(
      contentType = RenderThreadsContentTypes.LABEL,
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
            selectedColor = MaterialTheme.colors.primary,
        )

    val highAlpha = if (isEditable) ContentAlpha.high else ContentAlpha.disabled
    val mediumAlpha = if (isEditable) ContentAlpha.medium else ContentAlpha.disabled

    BetterSurface(
        modifier =
            Modifier.padding(top = MaterialTheme.keylines.content)
                .topBorder(
                    strokeWidth = 2.dp,
                    color =
                        MaterialTheme.colors.primary.copy(
                            alpha = mediumAlpha,
                        ),
                    cornerRadius = MaterialTheme.keylines.content,
                ),
        elevation = CardDefaults.Elevation,
        shape =
            MaterialTheme.shapes.medium.copy(
                bottomStart = ZeroCornerSize,
                bottomEnd = ZeroCornerSize,
            ),
    ) {
      Text(
          modifier = itemModifier.padding(MaterialTheme.keylines.content),
          text = "Thread Performance",
          style =
              MaterialTheme.typography.h6.copy(
                  fontWeight = FontWeight.W700,
                  color = cardColor.copy(alpha = highAlpha),
              ),
      )
    }
  }

  item(
      contentType = RenderThreadsContentTypes.EXPLAIN,
  ) {
    val mediumAlpha = if (isEditable) ContentAlpha.medium else ContentAlpha.disabled

    BetterSurface(
        modifier =
            Modifier.sideBorders(
                strokeWidth = 2.dp,
                color =
                    MaterialTheme.colors.primary.copy(
                        alpha = mediumAlpha,
                    ),
            ),
        elevation = CardDefaults.Elevation,
    ) {
      Text(
          modifier =
              itemModifier
                  .fillMaxWidth()
                  .padding(horizontal = MaterialTheme.keylines.content)
                  .padding(bottom = MaterialTheme.keylines.content * 2),
          text =
              """Adjust the Thread Pool constraints for $appName in the future here!"""
                  .trimMargin(),
          style =
              MaterialTheme.typography.caption.copy(
                  color = MaterialTheme.colors.onSurface.copy(alpha = mediumAlpha),
              ),
      )
    }
  }

  item(
      contentType = RenderThreadsContentTypes.ADJUST,
  ) {
    val mediumAlpha = if (isEditable) ContentAlpha.medium else ContentAlpha.disabled

    BetterSurface(
        modifier =
            Modifier.bottomBorder(
                strokeWidth = 2.dp,
                color =
                    MaterialTheme.colors.primary.copy(
                        alpha = mediumAlpha,
                    ),
                cornerRadius = MaterialTheme.keylines.content,
            ),
        elevation = CardDefaults.Elevation,
    ) {
      Button(
          modifier =
              itemModifier
                  .fillMaxWidth()
                  .padding(horizontal = MaterialTheme.keylines.content)
                  .padding(bottom = MaterialTheme.keylines.content),
          onClick = { /* TODO */},
      ) {
        Text(
            text = "Adjust Thread Pool",
        )
      }
    }
  }
}
