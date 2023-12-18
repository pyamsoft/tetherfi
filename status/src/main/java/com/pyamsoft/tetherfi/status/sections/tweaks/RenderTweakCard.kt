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

package com.pyamsoft.tetherfi.status.sections.tweaks

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.pydroid.ui.defaults.CardDefaults
import com.pyamsoft.pydroid.ui.util.bottomBorder
import com.pyamsoft.pydroid.ui.util.sideBorders
import com.pyamsoft.pydroid.ui.util.topBorder
import com.pyamsoft.tetherfi.status.StatusViewState
import com.pyamsoft.tetherfi.ui.BetterSurface
import com.pyamsoft.tetherfi.ui.checkable.rememberCheckableColor

private enum class RenderTweakCardContentTypes {
  LABEL,
  EXPLAIN,
  IGNORE_VPN,
  KILL_ON_IDLE,
  BIND_ALL
}

internal fun LazyListScope.renderTweakCard(
    itemModifier: Modifier = Modifier,
    appName: String,
    isEditable: Boolean,
    state: StatusViewState,
    onToggleIgnoreVpn: () -> Unit,
    onToggleShutdownWithNoClients: () -> Unit,
    onToggleBindProxyAll: () -> Unit,
) {
  item(
      contentType = RenderTweakCardContentTypes.LABEL,
  ) {
    val highAlpha = if (isEditable) ContentAlpha.high else ContentAlpha.disabled
    val mediumAlpha = if (isEditable) ContentAlpha.medium else ContentAlpha.disabled

    BetterSurface(
        modifier =
            Modifier.topBorder(
                strokeWidth = 2.dp,
                color =
                    MaterialTheme.colors.primary.copy(
                        alpha = mediumAlpha,
                    ),
                cornerRadius = MaterialTheme.keylines.content,
            ),
        elevation = CardDefaults.Elevation,
        shape =
            MaterialTheme.shapes.medium.copy(
                bottomEnd = ZeroCornerSize,
                bottomStart = ZeroCornerSize,
            ),
    ) {
      Text(
          modifier = itemModifier.padding(MaterialTheme.keylines.content),
          text = "Behavior Tweaks",
          style =
              MaterialTheme.typography.h6.copy(
                  fontWeight = FontWeight.W700,
                  color = MaterialTheme.colors.primary.copy(alpha = highAlpha),
              ),
      )
    }
  }

  item(
      contentType = RenderTweakCardContentTypes.EXPLAIN,
  ) {
    val mediumAlpha = if (isEditable) ContentAlpha.medium else ContentAlpha.disabled

    BetterSurface(
        modifier =
            Modifier.sideBorders(
                strokeWidth = 2.dp,
                color =
                    MaterialTheme.colors.primary.copy(
                        alpha = mediumAlpha,
                    ),
            ),
        elevation = CardDefaults.Elevation,
    ) {
      Text(
          modifier =
              itemModifier
                  .fillMaxWidth()
                  .padding(horizontal = MaterialTheme.keylines.content)
                  .padding(bottom = MaterialTheme.keylines.content * 2),
          text =
              """Tweaks change how $appName performs in various ways
                  |
                  |All of these options are completely optional and do not impact network or hotspot performance in any way."""
                  .trimMargin(),
          style =
              MaterialTheme.typography.caption.copy(
                  color = MaterialTheme.colors.onSurface.copy(alpha = mediumAlpha),
              ),
      )
    }
  }

  item(
      contentType = RenderTweakCardContentTypes.IGNORE_VPN,
  ) {
    val mediumAlpha = if (isEditable) ContentAlpha.medium else ContentAlpha.disabled
    val highAlpha = if (isEditable) ContentAlpha.high else ContentAlpha.disabled

    val isIgnoreVpn by state.isIgnoreVpn.collectAsStateWithLifecycle()
    val ignoreVpnColor by
        rememberCheckableColor(
            label = "Ignore VPN",
            condition = isIgnoreVpn,
            selectedColor = MaterialTheme.colors.primary,
        )

    BetterSurface(
        modifier =
            Modifier.sideBorders(
                strokeWidth = 2.dp,
                color =
                    MaterialTheme.colors.primary.copy(
                        alpha = mediumAlpha,
                    ),
            ),
        elevation = CardDefaults.Elevation,
    ) {
      ToggleSwitch(
          modifier = itemModifier,
          highAlpha = highAlpha,
          mediumAlpha = mediumAlpha,
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
    }
  }

  item(
      contentType = RenderTweakCardContentTypes.KILL_ON_IDLE,
  ) {
    val mediumAlpha = if (isEditable) ContentAlpha.medium else ContentAlpha.disabled
    val highAlpha = if (isEditable) ContentAlpha.high else ContentAlpha.disabled

    val isShutdownWithNoClients by state.isShutdownWithNoClients.collectAsStateWithLifecycle()
    val shutdownNoClientsColor by
        rememberCheckableColor(
            label = "Shutdown No Clients",
            condition = isShutdownWithNoClients,
            selectedColor = MaterialTheme.colors.primary,
        )

    BetterSurface(
        modifier =
            Modifier.sideBorders(
                strokeWidth = 2.dp,
                color =
                    MaterialTheme.colors.primary.copy(
                        alpha = mediumAlpha,
                    ),
            ),
        elevation = CardDefaults.Elevation,
    ) {
      ToggleSwitch(
          modifier = itemModifier,
          highAlpha = highAlpha,
          mediumAlpha = mediumAlpha,
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
    }
  }

  item(
      contentType = RenderTweakCardContentTypes.BIND_ALL,
  ) {
    val mediumAlpha = if (isEditable) ContentAlpha.medium else ContentAlpha.disabled
    val highAlpha = if (isEditable) ContentAlpha.high else ContentAlpha.disabled

    val isBindProxyAll by state.isBindProxyAll.collectAsStateWithLifecycle()
    val bindProxyAllColor by
        rememberCheckableColor(
            label = "Bind Proxy to All Interfaces",
            condition = isBindProxyAll,
            selectedColor = MaterialTheme.colors.primary,
        )

    BetterSurface(
        modifier =
            Modifier.bottomBorder(
                strokeWidth = 2.dp,
                color =
                    MaterialTheme.colors.primary.copy(
                        alpha = mediumAlpha,
                    ),
                cornerRadius = MaterialTheme.keylines.content,
            ),
        elevation = CardDefaults.Elevation,
        shape =
            MaterialTheme.shapes.medium.copy(
                topEnd = ZeroCornerSize,
                topStart = ZeroCornerSize,
            ),
    ) {
      ToggleSwitch(
          modifier = itemModifier,
          highAlpha = highAlpha,
          mediumAlpha = mediumAlpha,
          isEditable = isEditable,
          color = bindProxyAllColor,
          checked = isBindProxyAll,
          title = "Bind Proxy to All Interfaces",
          description =
              """If $appName has problems launching the Proxy but no problems with the Broadcast, you can try this tweak to have the hotspot bind to all interfaces, which might avoid the problem."""
                  .trimMargin(),
          onClick = onToggleBindProxyAll,
      )
    }
  }
}
