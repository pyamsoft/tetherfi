/*
 * Copyright 2025 pyamsoft
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

package com.pyamsoft.tetherfi.status.sections.network

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.VisualTransformation

internal enum class StatusEditorMode {
  STANDARD,
  OUTLINED,
}

@Composable
internal fun StatusEditor(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    mode: StatusEditorMode,
    title: String,
    value: String,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onChange: (String) -> Unit,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
  val textStyle =
      LocalTextStyle.current.copy(
          fontFamily = FontFamily.Monospace,
      )

  val label =
      @Composable {
        Text(
            text = title,
        )
      }

  when (mode) {
    StatusEditorMode.STANDARD -> {
      TextField(
          modifier = modifier,
          enabled = enabled,
          keyboardOptions = keyboardOptions,
          value = value,
          textStyle = textStyle,
          visualTransformation = visualTransformation,
          onValueChange = onChange,
          leadingIcon = leadingIcon,
          trailingIcon = trailingIcon,
          singleLine = true,
          label = label,
      )
    }
    StatusEditorMode.OUTLINED -> {
      OutlinedTextField(
          modifier = modifier,
          enabled = enabled,
          keyboardOptions = keyboardOptions,
          value = value,
          textStyle = textStyle,
          visualTransformation = visualTransformation,
          onValueChange = onChange,
          leadingIcon = leadingIcon,
          trailingIcon = trailingIcon,
          singleLine = true,
          label = label,
      )
    }
  }
}
