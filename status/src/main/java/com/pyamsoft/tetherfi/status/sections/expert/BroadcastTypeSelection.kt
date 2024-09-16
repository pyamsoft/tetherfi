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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.pydroid.ui.defaults.ImageDefaults
import com.pyamsoft.pydroid.ui.haptics.LocalHapticManager
import com.pyamsoft.pydroid.ui.icons.RadioButtonUnchecked
import com.pyamsoft.tetherfi.server.broadcast.BroadcastType
import com.pyamsoft.tetherfi.status.R
import com.pyamsoft.tetherfi.ui.ServerViewState
import com.pyamsoft.tetherfi.ui.checkable.rememberCheckableColor
import com.pyamsoft.tetherfi.ui.rememberCheckableIconColor
import com.pyamsoft.tetherfi.ui.textAlpha

@Composable
internal fun BroadcastTypeSelection(
    modifier: Modifier = Modifier,
    serverViewState: ServerViewState,
    appName: String,
    isEditable: Boolean,
    onSelectBroadcastType: (BroadcastType) -> Unit,
) {
  val currentBroadcastType by serverViewState.broadcastType.collectAsStateWithLifecycle()
  val allBroadcastTypes = remember { BroadcastType.entries.toTypedArray() }
  Column(
      modifier = modifier,
  ) {
    Text(
        modifier = Modifier.padding(horizontal = MaterialTheme.keylines.content),
        text = stringResource(R.string.expert_broadcast_title),
        style =
            MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.W700,
                color =
                    MaterialTheme.colorScheme.primary.copy(
                        alpha = textAlpha(isEditable),
                    ),
            ),
    )
    Text(
        modifier = Modifier.padding(horizontal = MaterialTheme.keylines.content),
        text = stringResource(R.string.expert_broadcast_description, appName),
        style =
            MaterialTheme.typography.bodyMedium.copy(
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = textAlpha(isEditable),
                    ),
            ),
    )

    Row(
        modifier = Modifier.height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
      for (type in allBroadcastTypes) {
        SelectableBroadcastType(
            modifier = Modifier.weight(1F).fillMaxHeight(),
            type = type,
            currentSelectedType = currentBroadcastType,
            isEditable = isEditable,
            onClick = onSelectBroadcastType,
        )
      }
    }
  }
}

@Composable
private fun SelectableBroadcastType(
    modifier: Modifier = Modifier,
    type: BroadcastType,
    currentSelectedType: BroadcastType?,
    isEditable: Boolean,
    onClick: (BroadcastType) -> Unit,
) {
  val hapticManager = LocalHapticManager.current
  val isSelected = remember(type, currentSelectedType) { type == currentSelectedType }
  val iconColor = rememberCheckableIconColor(isEditable, isSelected)

  val titleRes =
      remember(type) {
        when (type) {
          BroadcastType.WIFI_DIRECT -> R.string.expert_broadcast_type_wifi_direct_title
          BroadcastType.RNDIS -> R.string.expert_broadcast_type_rndis_title
        }
      }

  val descriptionRes =
      remember(type) {
        when (type) {
          BroadcastType.WIFI_DIRECT -> R.string.expert_broadcast_type_wifi_direct_description
          BroadcastType.RNDIS -> R.string.expert_broadcast_type_rndis_description
        }
      }

  val title = stringResource(titleRes)
  val description = stringResource(descriptionRes)

  val color by
      rememberCheckableColor(
          enabled = isEditable,
          label = title,
          condition = isSelected,
          selectedColor = MaterialTheme.colorScheme.primary,
      )

  val checkIcon =
      remember(isSelected) {
        if (isSelected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked
      }

  Column(
      modifier =
          modifier
              .clickable(enabled = isEditable) {
                hapticManager?.toggleOn()
                onClick(type)
              }
              .padding(MaterialTheme.keylines.content),
  ) {
    Row(
        verticalAlignment = Alignment.Top,
    ) {
      Text(
          modifier = Modifier.weight(1F).padding(bottom = MaterialTheme.keylines.baseline),
          text = title,
          style =
              MaterialTheme.typography.bodyLarge.copy(
                  fontWeight = FontWeight.W700,
                  color =
                      color.copy(
                          alpha = textAlpha(isEditable),
                      ),
              ),
      )

      Icon(
          modifier = Modifier.size(ImageDefaults.IconSize),
          imageVector = checkIcon,
          contentDescription = title,
          tint = iconColor,
      )
    }

    Text(
        text = description,
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
