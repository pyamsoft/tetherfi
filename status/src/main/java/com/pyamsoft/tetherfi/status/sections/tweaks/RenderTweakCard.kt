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

package com.pyamsoft.tetherfi.status.sections.tweaks

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.tetherfi.status.R
import com.pyamsoft.tetherfi.status.StatusViewState
import com.pyamsoft.tetherfi.ui.checkable.rememberCheckableColor
import com.pyamsoft.tetherfi.ui.textAlpha

private enum class RenderTweakCardContentTypes {
  BEHAVIOR_TWEAKS,
}

internal fun LazyListScope.renderTweakCard(
    itemModifier: Modifier = Modifier,
    appName: String,
    isEditable: Boolean,
    state: StatusViewState,
    onToggleIgnoreVpn: () -> Unit,
    onToggleIgnoreLocation: () -> Unit,
    onToggleShutdownWithNoClients: () -> Unit,
    onToggleKeepScreenOn: () -> Unit,
) {
  item(
      contentType = RenderTweakCardContentTypes.BEHAVIOR_TWEAKS,
  ) {
    Card(
        modifier = itemModifier,
        shape = MaterialTheme.shapes.medium,
        border =
            BorderStroke(
                width = 2.dp,
                color = MaterialTheme.colorScheme.primaryContainer,
            ),
    ) {
      Column(
          modifier = Modifier.padding(vertical = MaterialTheme.keylines.content),
      ) {
        Text(
            modifier =
                Modifier.padding(horizontal = MaterialTheme.keylines.content)
                    .padding(bottom = MaterialTheme.keylines.content),
            text = stringResource(R.string.tweaks_title),
            style =
                MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.W700,
                    color = MaterialTheme.colorScheme.primary,
                ),
        )

        Text(
            modifier =
                Modifier.fillMaxWidth()
                    .padding(horizontal = MaterialTheme.keylines.content)
                    .padding(bottom = MaterialTheme.keylines.content),
            text = stringResource(R.string.tweaks_description, appName),
            style =
                MaterialTheme.typography.bodyMedium.copy(
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = textAlpha(isEditable),
                        ),
                ),
        )

        SocketTimeoutTweak(
            modifier = Modifier.fillMaxWidth(),
            appName = appName,
            isEditable = isEditable,
        )

        KeepScreenOnTweak(
            modifier = Modifier.fillMaxWidth(),
            isEditable = isEditable,
            state = state,
            onToggleKeepScreenOn = onToggleKeepScreenOn,
        )

        LocationBlockerTweak(
            modifier = Modifier.fillMaxWidth(),
            appName = appName,
            isEditable = isEditable,
            state = state,
            onToggleIgnoreLocation = onToggleIgnoreLocation,
        )

        VPNBlockerTweak(
            modifier = Modifier.fillMaxWidth(),
            appName = appName,
            isEditable = isEditable,
            state = state,
            onToggleIgnoreVpn = onToggleIgnoreVpn,
        )

        StopHotspotTweak(
            modifier = Modifier.fillMaxWidth(),
            appName = appName,
            isEditable = isEditable,
            state = state,
            onToggleShutdownWithNoClients = onToggleShutdownWithNoClients,
        )
      }
    }
  }
}

@Composable
private fun SocketTimeoutTweak(
    modifier: Modifier = Modifier,
    appName: String,
    isEditable: Boolean,
) {
  val color by
      rememberCheckableColor(
          enabled = isEditable,
          label = "Enable Socket Timeout",
          condition = true,
          selectedColor = MaterialTheme.colorScheme.primary,
      )

  ToggleSwitch(
      modifier = modifier,
      isEditable = isEditable,
      color = color,
      checked = true,
      title = stringResource(R.string.enable_socket_timeout_title),
      description = stringResource(R.string.enable_socket_timeout_description, appName),
      onClick = {},
  )
}

@Composable
private fun LocationBlockerTweak(
    modifier: Modifier = Modifier,
    appName: String,
    isEditable: Boolean,
    state: StatusViewState,
    onToggleIgnoreLocation: () -> Unit,
) {
  val isIgnoreLocation by state.isIgnoreLocation.collectAsStateWithLifecycle()
  val color by
      rememberCheckableColor(
          enabled = isEditable,
          label = "Ignore Location",
          condition = isIgnoreLocation,
          selectedColor = MaterialTheme.colorScheme.primary,
      )

  ToggleSwitch(
      modifier = modifier,
      isEditable = isEditable,
      color = color,
      checked = isIgnoreLocation,
      title = stringResource(R.string.ignore_location_title),
      description = stringResource(R.string.ignore_location_description, appName),
      onClick = onToggleIgnoreLocation,
  )
}

@Composable
private fun VPNBlockerTweak(
    modifier: Modifier = Modifier,
    appName: String,
    isEditable: Boolean,
    state: StatusViewState,
    onToggleIgnoreVpn: () -> Unit,
) {
  val isIgnoreVpn by state.isIgnoreVpn.collectAsStateWithLifecycle()
  val color by
      rememberCheckableColor(
          enabled = isEditable,
          label = "Ignore VPN",
          condition = isIgnoreVpn,
          selectedColor = MaterialTheme.colorScheme.primary,
      )

  ToggleSwitch(
      modifier = modifier,
      isEditable = isEditable,
      color = color,
      checked = isIgnoreVpn,
      title = stringResource(R.string.ignore_vpn_title),
      description = stringResource(R.string.ignore_vpn_description, appName),
      onClick = onToggleIgnoreVpn,
  )
}

@Composable
private fun StopHotspotTweak(
    modifier: Modifier = Modifier,
    appName: String,
    isEditable: Boolean,
    state: StatusViewState,
    onToggleShutdownWithNoClients: () -> Unit,
) {
  val isShutdownWithNoClients by state.isShutdownWithNoClients.collectAsStateWithLifecycle()
  val color by
      rememberCheckableColor(
          enabled = isEditable,
          label = "Shutdown No Clients",
          condition = isShutdownWithNoClients,
          selectedColor = MaterialTheme.colorScheme.primary,
      )

  ToggleSwitch(
      modifier = modifier,
      isEditable = isEditable,
      color = color,
      checked = isShutdownWithNoClients,
      title = stringResource(R.string.shutdown_no_client_title),
      description = stringResource(R.string.shutdown_no_client_description, appName),
      onClick = onToggleShutdownWithNoClients,
  )
}

@Composable
private fun KeepScreenOnTweak(
    modifier: Modifier = Modifier,
    isEditable: Boolean,
    state: StatusViewState,
    onToggleKeepScreenOn: () -> Unit,
) {
  val isKeepScreenOn by state.isKeepScreenOn.collectAsStateWithLifecycle()
  val color by
      rememberCheckableColor(
          enabled = isEditable,
          label = "Keep Screen On",
          condition = isKeepScreenOn,
          selectedColor = MaterialTheme.colorScheme.primary,
      )

  ToggleSwitch(
      modifier = modifier,
      isEditable = isEditable,
      color = color,
      checked = isKeepScreenOn,
      title = stringResource(R.string.keep_screen_on_title),
      description = stringResource(R.string.keep_screen_on_description),
      onClick = onToggleKeepScreenOn,
  )
}
