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

package com.pyamsoft.tetherfi.status.sections.expert

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.tetherfi.server.broadcast.BroadcastType
import com.pyamsoft.tetherfi.status.R
import com.pyamsoft.tetherfi.ui.ServerViewState
import com.pyamsoft.tetherfi.ui.textAlpha

@Composable
internal fun ExpertSettings(
    modifier: Modifier = Modifier,
    isEditable: Boolean,
    serverViewState: ServerViewState,
    appName: String,

    // Power Balance
    onShowPowerBalance: () -> Unit,

    // Broadcast type
    onSelectBroadcastType: (BroadcastType) -> Unit,
) {
  Card(
      modifier = modifier,
      border =
          BorderStroke(
              width = 2.dp,
              color = MaterialTheme.colorScheme.primaryContainer,
          ),
      shape = MaterialTheme.shapes.medium,
  ) {
    Column {
      Text(
          modifier =
              Modifier.padding(horizontal = MaterialTheme.keylines.content)
                  .padding(
                      top = MaterialTheme.keylines.content,
                      bottom = MaterialTheme.keylines.typography,
                  ),
          text = stringResource(R.string.expert_title),
          style =
              MaterialTheme.typography.titleLarge.copy(
                  fontWeight = FontWeight.W700,
                  color =
                      MaterialTheme.colorScheme.primary.copy(
                          alpha = textAlpha(isEditable),
                      ),
              ),
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

      PowerBalance(
          modifier = Modifier.padding(MaterialTheme.keylines.content),
          isEditable = isEditable,
          appName = appName,
          onShowPowerBalance = onShowPowerBalance,
      )

      BroadcastTypeSelection(
          serverViewState = serverViewState,
          appName = appName,
          isEditable = isEditable,
          onSelectBroadcastType = onSelectBroadcastType,
      )
    }
  }
}
