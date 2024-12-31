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

package com.pyamsoft.tetherfi.status

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.tetherfi.server.status.RunningStatus
import com.pyamsoft.tetherfi.status.sections.StatusItem

internal enum class StatusSize {
  SMALL,
  NORMAL
}

@Composable
internal fun DisplayStatus(
    modifier: Modifier = Modifier,
    title: String,
    status: RunningStatus,
    size: StatusSize,
    onClickShowError: (() -> Unit)? = null,
) {
  val context = LocalContext.current
  val text =
      remember(
          status,
          context,
      ) {
        when (status) {
          is RunningStatus.Error ->
              status.throwable.message ?: context.getString(R.string.status_error)
          is RunningStatus.NotRunning -> context.getString(R.string.status_not_running)
          is RunningStatus.Running -> context.getString(R.string.status_running)
          is RunningStatus.Starting -> context.getString(R.string.status_starting)
          is RunningStatus.Stopping -> context.getString(R.string.status_stopping)
        }
      }

  val textErrorColor = MaterialTheme.colorScheme.onErrorContainer
  val textRunningColor = MaterialTheme.colorScheme.onTertiaryContainer
  val textProgressColor = MaterialTheme.colorScheme.onSecondaryContainer
  val textNothingColor = MaterialTheme.colorScheme.onSurfaceVariant
  val textColor =
      remember(
          size, status, textErrorColor, textRunningColor, textProgressColor, textNothingColor) {
            when (status) {
              is RunningStatus.Error -> textErrorColor
              is RunningStatus.Running -> textRunningColor
              is RunningStatus.NotRunning -> textNothingColor
              is RunningStatus.Starting,
              is RunningStatus.Stopping -> {
                when (size) {
                  StatusSize.SMALL -> textNothingColor
                  StatusSize.NORMAL -> textProgressColor
                }
              }
            }
          }

  val smallStyle = MaterialTheme.typography.bodyLarge
  val normalStyle = MaterialTheme.typography.headlineSmall
  val valueStyle =
      remember(
          smallStyle,
          normalStyle,
          size,
      ) {
        when (size) {
          StatusSize.SMALL -> smallStyle
          StatusSize.NORMAL -> normalStyle
        }
      }

  val backgroundErrorColor = MaterialTheme.colorScheme.errorContainer
  val backgroundRunningColor = MaterialTheme.colorScheme.tertiaryContainer
  val backgroundProgressColor = MaterialTheme.colorScheme.secondaryContainer
  val backgroundColor =
      remember(
          size, status, backgroundErrorColor, backgroundProgressColor, backgroundRunningColor) {
            when (size) {
              StatusSize.SMALL -> Color.Unspecified
              StatusSize.NORMAL ->
                  when (status) {
                    is RunningStatus.Error -> backgroundErrorColor
                    is RunningStatus.Running -> backgroundRunningColor
                    is RunningStatus.Starting -> backgroundProgressColor
                    is RunningStatus.Stopping -> backgroundProgressColor
                    is RunningStatus.NotRunning -> Color.Unspecified
                  }
            }
          }

  val borderColor =
      remember(
          size,
          backgroundColor,
      ) {
        when (size) {
          StatusSize.SMALL -> Color.Unspecified
          StatusSize.NORMAL -> backgroundColor
        }
      }

  val showError =
      remember(
          onClickShowError,
          status,
      ) {
        if (status is RunningStatus.Error) onClickShowError else null
      }

  StatusItem(
      modifier = modifier,
      title = title,
      value = text,
      color = textColor,
      showError = showError,
      valueModifier =
          Modifier.run {
                when (size) {
                  StatusSize.SMALL -> this
                  StatusSize.NORMAL -> padding(top = MaterialTheme.keylines.typography)
                }
              }
              .border(
                  width = 1.dp,
                  color = borderColor,
                  shape = MaterialTheme.shapes.small,
              )
              .background(
                  color = backgroundColor,
                  shape = MaterialTheme.shapes.small,
              )
              .run {
                when (size) {
                  StatusSize.SMALL -> this
                  StatusSize.NORMAL ->
                      padding(
                          horizontal = MaterialTheme.keylines.baseline,
                          vertical = MaterialTheme.keylines.typography,
                      )
                }
              },
      valueStyle =
          valueStyle.copy(
              fontWeight = FontWeight.W400,
              textAlign = TextAlign.Center,
          ),
  )
}
