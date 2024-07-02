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
    onToggleShutdownWithNoClients: () -> Unit,
    onToggleSocketTimeout: () -> Unit,
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

        val isSocketTimeoutEnabled by state.isSocketTimeoutEnabled.collectAsStateWithLifecycle()
        val socketTimeoutColor by
            rememberCheckableColor(
                enabled = isEditable,
                label = "Enable Socket Timeout",
                condition = isSocketTimeoutEnabled,
                selectedColor = MaterialTheme.colorScheme.primary,
            )

        ToggleSwitch(
            modifier = Modifier.fillMaxWidth(),
            isEditable = isEditable,
            color = socketTimeoutColor,
            checked = isSocketTimeoutEnabled,
            title = stringResource(R.string.enable_socket_timeout_title),
            description = stringResource(R.string.enable_socket_timeout_description, appName),
            onClick = onToggleSocketTimeout,
        )

        val isIgnoreVpn by state.isIgnoreVpn.collectAsStateWithLifecycle()
        val ignoreVpnColor by
            rememberCheckableColor(
                enabled = isEditable,
                label = "Ignore VPN",
                condition = isIgnoreVpn,
                selectedColor = MaterialTheme.colorScheme.primary,
            )

        ToggleSwitch(
            modifier = Modifier.fillMaxWidth(),
            isEditable = isEditable,
            color = ignoreVpnColor,
            checked = isIgnoreVpn,
            title = stringResource(R.string.ignore_vpn_title),
            description = stringResource(R.string.ignore_vpn_description, appName),
            onClick = onToggleIgnoreVpn,
        )

        val isShutdownWithNoClients by state.isShutdownWithNoClients.collectAsStateWithLifecycle()
        val shutdownNoClientsColor by
            rememberCheckableColor(
                enabled = isEditable,
                label = "Shutdown No Clients",
                condition = isShutdownWithNoClients,
                selectedColor = MaterialTheme.colorScheme.primary,
            )

        ToggleSwitch(
            modifier = Modifier.fillMaxWidth(),
            isEditable = isEditable,
            color = shutdownNoClientsColor,
            checked = isShutdownWithNoClients,
            title = stringResource(R.string.shutdown_no_client_title),
            description = stringResource(R.string.shutdown_no_client_description, appName),
            onClick = onToggleShutdownWithNoClients,
        )
      }
    }
  }
}
