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

package com.pyamsoft.tetherfi.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.pydroid.theme.keylines

@Composable
fun Label(
    modifier: Modifier = Modifier,
    text: String,
    color: Color = MaterialTheme.colors.onBackground,
    onAction: (() -> Unit)? = null,
) {
  Row(
      modifier = modifier,
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
  ) {
    Text(
        text = text,
        style =
            MaterialTheme.typography.caption.copy(
                fontWeight = FontWeight.W700,
                color =
                    color.copy(
                        alpha = ContentAlpha.medium,
                    ),
            ),
    )

    onAction.also { click ->
      AnimatedVisibility(
          visible = click != null,
      ) {
        Icon(
            modifier =
                Modifier.padding(start = MaterialTheme.keylines.content).size(24.dp).clickable {
                  click.requireNotNull().invoke()
                },
            imageVector = Icons.Filled.Info,
            contentDescription = "View Information",
        )
      }
    }
  }
}
