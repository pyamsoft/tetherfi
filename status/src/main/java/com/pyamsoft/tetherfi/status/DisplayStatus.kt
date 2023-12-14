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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.pydroid.theme.success
import com.pyamsoft.tetherfi.server.status.RunningStatus
import com.pyamsoft.tetherfi.status.common.StatusItem

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
  val text =
      remember(status) {
        when (status) {
          is RunningStatus.Error -> status.throwable.message ?: "An unexpected error occurred."
          is RunningStatus.NotRunning -> "Not Running"
          is RunningStatus.Running -> "Running"
          is RunningStatus.Starting -> "Starting"
          is RunningStatus.Stopping -> "Stopping"
        }
      }

  val errorColor = MaterialTheme.colors.error
  val runningColor = MaterialTheme.colors.success
  val color =
      remember(
          status,
          errorColor,
          runningColor,
      ) {
        when (status) {
          is RunningStatus.Error -> errorColor
          is RunningStatus.NotRunning -> Color.Unspecified
          is RunningStatus.Running -> runningColor
          is RunningStatus.Starting -> Color.Cyan
          is RunningStatus.Stopping -> Color.Magenta
        }
      }

  val smallStyle = MaterialTheme.typography.body1
  val normalStyle = MaterialTheme.typography.h5
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

  val bgColor =
      remember(
          size,
          color,
      ) {
        when (size) {
          StatusSize.SMALL -> Color.Unspecified
          StatusSize.NORMAL -> color
        }
      }

  val backgroundColor = MaterialTheme.colors.onBackground
  val borderColor =
      remember(
          size,
          color,
          backgroundColor,
      ) {
        when (size) {
          StatusSize.SMALL -> Color.Unspecified
          StatusSize.NORMAL -> if (color.isUnspecified) backgroundColor else color
        }
      }

  val fgColor =
      remember(
          size,
          color,
      ) {
        when (size) {
          StatusSize.SMALL -> color
          StatusSize.NORMAL -> Color.Unspecified
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
      color = fgColor,
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
                  color = bgColor,
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
