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

package com.pyamsoft.tetherfi.status.common

import androidx.compose.foundation.layout.Column
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.pyamsoft.tetherfi.ui.Label

@Composable
internal fun StatusItem(
    modifier: Modifier = Modifier,
    valueModifier: Modifier = Modifier,
    title: String,
    value: String,
    showError: (() -> Unit)? = null,
    valueStyle: TextStyle = MaterialTheme.typography.body1,
    color: Color = Color.Unspecified,
) {
  Column(
      modifier = modifier,
  ) {
    Label(
        text = title,
        color = MaterialTheme.colors.onSurface,
        onAction = showError,
    )
    Text(
        modifier = valueModifier,
        text = value,
        style = valueStyle,
        color = color,
    )
  }
}
