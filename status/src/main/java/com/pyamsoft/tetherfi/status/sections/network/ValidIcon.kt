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

package com.pyamsoft.tetherfi.status.sections.network

import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.pyamsoft.pydroid.theme.success

@Composable
internal fun ValidIcon(
    modifier: Modifier = Modifier,
    isValid: Boolean,
    what: String,
) {
  val icon = remember(isValid) { if (isValid) Icons.Filled.Check else Icons.Filled.Close }
  val description =
      remember(
          isValid,
          what,
      ) {
        "$what is ${if (isValid) "Valid" else "Invalid"}"
      }
  val tint = if (isValid) MaterialTheme.colors.success else MaterialTheme.colors.error

  Icon(
      modifier = modifier,
      imageVector = icon,
      tint = tint,
      contentDescription = description,
  )
}
