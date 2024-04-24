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

package com.pyamsoft.tetherfi.ui.checkable

import androidx.annotation.CheckResult
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.pydroid.ui.defaults.ImageDefaults
import com.pyamsoft.pydroid.ui.icons.RadioButtonUnchecked
import com.pyamsoft.pydroid.ui.theme.ZeroSize

@Composable
@CheckResult
fun rememberCheckableColor(
    label: String,
    condition: Boolean,
    selectedColor: Color,
): State<Color> {
  val unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
  val color =
      remember(
          condition,
          unselectedColor,
          selectedColor,
      ) {
        if (condition) selectedColor else unselectedColor
      }
  return animateColorAsState(
      label = label,
      targetValue = color,
  )
}

@Composable
@CheckResult
internal fun rememberCheckableIconColor(condition: Boolean): Color {
  val unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
  val selectedColor = MaterialTheme.colorScheme.primary
  return remember(
      condition,
      unselectedColor,
      selectedColor,
  ) {
    if (condition) selectedColor else unselectedColor
  }
}

/** Fancy checkable with Material Design ish elements */
@Composable
fun CheckableCard(
    modifier: Modifier = Modifier,
    isEditable: Boolean,
    condition: Boolean,
    title: String,
    description: String,
    onClick: () -> Unit,
    /** Hack to make two different cards the same size based on their content */
    extraHeight: Dp = ZeroSize,
) {
  CheckableCard(
      modifier = modifier,
      isEditable = isEditable,
      condition = condition,
      title = title,
      description = description,
      titleColor = MaterialTheme.colorScheme.primary,
      borderColor = MaterialTheme.colorScheme.primaryContainer,
      extraHeight = extraHeight,
      onClick = onClick,
  )
}

@Composable
private fun CheckableCard(
    modifier: Modifier = Modifier,
    isEditable: Boolean,
    condition: Boolean,
    title: String,
    description: String,
    titleColor: Color,
    borderColor: Color,
    extraHeight: Dp,
    onClick: () -> Unit,
) {
  val iconColor = rememberCheckableIconColor(condition)
  val color by
      rememberCheckableColor(
          label = title,
          condition = condition,
          selectedColor = titleColor,
      )

  val checkIcon =
      remember(condition) {
        if (condition) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked
      }

  Card(
      modifier = modifier,
      border =
          BorderStroke(
              width = 2.dp,
              color = borderColor,
          ),
      shape = MaterialTheme.shapes.medium,
  ) {
    Column(
        modifier =
            Modifier.clickable(enabled = isEditable) { onClick() }
                .padding(MaterialTheme.keylines.content),
    ) {
      Row(
          verticalAlignment = Alignment.Top,
      ) {
        Text(
            modifier = Modifier.weight(1F).padding(bottom = MaterialTheme.keylines.baseline),
            text = title,
            style =
                MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.W700,
                    color = color,
                ),
        )

        Icon(
            modifier = Modifier.size(ImageDefaults.IconSize),
            imageVector = checkIcon,
            contentDescription = title,
            tint = iconColor,
        )
      }

      // Align with the largest card
      if (extraHeight > ZeroSize) {
        Spacer(
            modifier = Modifier.height(extraHeight),
        )
      }

      Text(
          text = description,
          style =
              MaterialTheme.typography.bodyMedium.copy(
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
              ),
      )
    }
  }
}
