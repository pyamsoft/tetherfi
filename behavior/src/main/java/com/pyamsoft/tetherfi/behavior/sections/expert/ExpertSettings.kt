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

package com.pyamsoft.tetherfi.behavior.sections.expert

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.tetherfi.behavior.R
import com.pyamsoft.tetherfi.ui.Label
import com.pyamsoft.tetherfi.ui.textAlpha

@Composable
internal fun ExpertSettings(
    modifier: Modifier = Modifier,
    isEditable: Boolean,
    appName: String,
) {
  Column(
      modifier = modifier,
  ) {
    Label(
        modifier =
            Modifier.align(Alignment.CenterHorizontally)
                .padding(horizontal = MaterialTheme.keylines.content)
                .padding(
                    top = MaterialTheme.keylines.content,
                    bottom = MaterialTheme.keylines.typography,
                ),
        text = stringResource(R.string.expert_title),
    )
    Text(
        modifier = Modifier.padding(horizontal = MaterialTheme.keylines.content),
        text = stringResource(R.string.expert_description, appName),
        style =
            MaterialTheme.typography.bodyMedium.copy(
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = textAlpha(isEditable),
                    ),
            ),
    )
  }
}
