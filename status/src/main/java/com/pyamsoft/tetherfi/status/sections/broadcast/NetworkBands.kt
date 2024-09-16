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

package com.pyamsoft.tetherfi.status.sections.broadcast

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.pydroid.ui.haptics.LocalHapticManager
import com.pyamsoft.pydroid.ui.theme.ZeroSize
import com.pyamsoft.tetherfi.server.ServerDefaults
import com.pyamsoft.tetherfi.server.ServerNetworkBand
import com.pyamsoft.tetherfi.server.broadcast.BroadcastType
import com.pyamsoft.tetherfi.status.R
import com.pyamsoft.tetherfi.status.StatusViewState
import com.pyamsoft.tetherfi.ui.Label
import com.pyamsoft.tetherfi.ui.ServerViewState
import com.pyamsoft.tetherfi.ui.checkable.CheckableCard
import com.pyamsoft.tetherfi.ui.surfaceAlpha

@Composable
internal fun NetworkBands(
    modifier: Modifier = Modifier,
    isEditable: Boolean,
    state: StatusViewState,
    serverViewState: ServerViewState,
    onSelectBand: (ServerNetworkBand) -> Unit,
) {
  val hapticManager = LocalHapticManager.current
  val band by state.band.collectAsStateWithLifecycle()
  val canUseCustomConfig = remember { ServerDefaults.canUseCustomConfig() }
  val broadcastType by serverViewState.broadcastType.collectAsStateWithLifecycle()

  // Render only if Wifi Direct
  if (broadcastType != BroadcastType.WIFI_DIRECT) {
    return
  }

  Column(
      modifier = modifier,
  ) {

    // Small label above
    Label(
        modifier = Modifier.fillMaxWidth().padding(bottom = MaterialTheme.keylines.baseline),
        text = stringResource(R.string.broadcast_frequency),
    )
    if (canUseCustomConfig) {
      val bands = remember { ServerNetworkBand.entries }
      val bandIterator = remember(bands) { bands.withIndex() }

      // Then the buttons
      Row(
          modifier = Modifier.height(IntrinsicSize.Min),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceEvenly,
      ) {
        for ((index, b) in bandIterator) {
          SelectableNetworkBand(
              modifier =
                  Modifier.weight(1F)
                      .fillMaxHeight()
                      .padding(
                          end =
                              if (index < bands.lastIndex) MaterialTheme.keylines.content
                              else ZeroSize,
                      ),
              isEditable = isEditable,
              band = b,
              currentSelectedBand = band,
              onSelectBand = onSelectBand,
          )
        }
      }
    } else {
      Text(
          modifier =
              Modifier.border(
                      width = 2.dp,
                      color =
                          MaterialTheme.colorScheme.secondary.copy(
                              alpha = surfaceAlpha(isEditable),
                          ),
                      shape = MaterialTheme.shapes.medium,
                  )
                  .background(
                      color =
                          MaterialTheme.colorScheme.secondaryContainer.copy(
                              alpha = surfaceAlpha(isEditable),
                          ),
                      shape = MaterialTheme.shapes.medium,
                  )
                  .padding(MaterialTheme.keylines.content),
          text = stringResource(R.string.network_bands_system_defined),
          style =
              MaterialTheme.typography.bodyLarge.copy(
                  fontWeight = FontWeight.W700,
                  color =
                      MaterialTheme.colorScheme.onSecondaryContainer.copy(
                          alpha = surfaceAlpha(isEditable),
                      ),
              ),
      )
    }
  }
}

@Composable
private fun SelectableNetworkBand(
    modifier: Modifier,
    isEditable: Boolean,
    band: ServerNetworkBand,
    currentSelectedBand: ServerNetworkBand?,
    onSelectBand: (ServerNetworkBand) -> Unit,
) {
  val hapticManager = LocalHapticManager.current
  val isSelected = remember(band, currentSelectedBand) { band == currentSelectedBand }

  val titleRes =
      remember(band) {
        when (band) {
          ServerNetworkBand.LEGACY -> R.string.network_bands_legacy_title
          ServerNetworkBand.MODERN -> R.string.network_bands_modern_title
        }
      }

  val descriptionRes =
      remember(band) {
        when (band) {
          ServerNetworkBand.LEGACY -> R.string.network_bands_legacy_description
          ServerNetworkBand.MODERN -> R.string.network_bands_modern_description
        }
      }

  val title = stringResource(titleRes)
  val description = stringResource(descriptionRes)

  CheckableCard(
      modifier = modifier,
      isEditable = isEditable,
      condition = isSelected,
      title = title,
      description = description,
      onClick = {
        hapticManager?.toggleOn()
        onSelectBand(band)
      },
  )
}
