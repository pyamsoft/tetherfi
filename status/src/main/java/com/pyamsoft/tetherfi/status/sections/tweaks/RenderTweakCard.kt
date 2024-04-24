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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pyamsoft.pydroid.theme.keylines
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
    onToggleProxyYolo: () -> Unit,
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
            text = "Behavior Tweaks",
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
            text =
                """Tweaks change how $appName performs in various ways
                  |
                  |All of these options are completely optional and do not impact network or hotspot performance in any way."""
                    .trimMargin(),
            style =
                MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = textAlpha(isEditable),
                    ),
                ),
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
            title = "Avoid VPN Blocker Dialog",
            description =
                """When starting, $appName sometimes has trouble if a VPN is running, and will refuse to start the hotspot until it is turned off.
                  |
                  |If you KNOW your VPN app works fine with $appName, turn this option on to avoid the blocking dialog."""
                    .trimMargin(),
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
            title = "Stop Hotspot With No Clients",
            description =
                """If the $appName hotspot has been running for 10 minutes without serving any client devices, shut it down.
                  |
                  |Automatically shutting down the hotspot when it is not being used can save battery."""
                    .trimMargin(),
            onClick = onToggleShutdownWithNoClients,
        )

        val proxyBindAllColor by
            rememberCheckableColor(
                enabled = isEditable,
                label = "Bind Proxy to All Interfaces",
                condition = false,
                selectedColor = MaterialTheme.colorScheme.primary,
            )

        ToggleSwitch(
            modifier = Modifier.fillMaxWidth(),
            isEditable = isEditable,
            color = proxyBindAllColor,
            checked = false,
            title = "Bind Proxy to All Interfaces",
            description =
                """This Tweak is no longer supported - the default Hotspot configuration is MORE performant and MORE reliable.
                    |
                    |This Tweak will be removed in a future update.
                """
                    .trimMargin(),
            onClick = {},
        )

        val isProxyYolo by state.isProxyYolo.collectAsStateWithLifecycle()
        val proxyYoloColor by
            rememberCheckableColor(
                enabled = isEditable,
                label = "Stubborn Proxy",
                condition = isProxyYolo,
                selectedColor = MaterialTheme.colorScheme.primary,
            )

        ToggleSwitch(
            modifier = Modifier.fillMaxWidth(),
            isEditable = isEditable,
            color = proxyYoloColor,
            checked = isProxyYolo,
            title = "Stubborn Proxy",
            description =
                """On some devices $appName fails to launch the Proxy with an 'Invalid Argument' error. In some cases, this error is "not real" and can be recovered from by being stubborn and trying again and again.
                    |
                    |Enabling this option lets $appName ignore these errors and constantly keep trying. YOLO Mode.
                    |
                    |This option will be made Default in a future version."""
                    .trimMargin(),
            onClick = onToggleProxyYolo,
        )
      }
    }
  }
}
