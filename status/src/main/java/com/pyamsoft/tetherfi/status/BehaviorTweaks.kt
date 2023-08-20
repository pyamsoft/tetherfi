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

package com.pyamsoft.tetherfi.status

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.Card
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.pydroid.ui.defaults.CardDefaults
import com.pyamsoft.tetherfi.ui.Label
import com.pyamsoft.tetherfi.ui.checkable.rememberCheckableColor

internal fun LazyListScope.renderTweaks(
    itemModifier: Modifier = Modifier,
    isEditable: Boolean,
    appName: String,
    state: StatusViewState,
    onToggleIgnoreVpn: () -> Unit,
) {
  item(
      contentType = StatusScreenContentTypes.TWEAK_LABEL,
  ) {
    Label(
        modifier =
            itemModifier
                .padding(top = MaterialTheme.keylines.content)
                .padding(bottom = MaterialTheme.keylines.baseline),
        text = "Tweaks",
    )
  }

  item(
      contentType = StatusScreenContentTypes.TWEAKS,
  ) {
    BehaviorTweaks(
        modifier = itemModifier,
        isEditable = isEditable,
        appName = appName,
        state = state,
        onToggleIgnoreVpn = onToggleIgnoreVpn,
    )
  }
}

@Composable
private fun BehaviorTweaks(
    modifier: Modifier = Modifier,
    appName: String,
    isEditable: Boolean,
    state: StatusViewState,
    onToggleIgnoreVpn: () -> Unit,
) {
  val isIgnoreVpn by state.isIgnoreVpn.collectAsStateWithLifecycle()

  val ignoreVpnColor by
      rememberCheckableColor(
          label = "Ignore VPN",
          condition = isIgnoreVpn,
          selectedColor = MaterialTheme.colors.primary,
      )

  val highAlpha = if (isEditable) ContentAlpha.high else ContentAlpha.disabled
  val mediumAlpha = if (isEditable) ContentAlpha.medium else ContentAlpha.disabled

  Card(
      modifier =
          modifier.border(
              width = 2.dp,
              color = MaterialTheme.colors.primary.copy(alpha = mediumAlpha),
              shape = MaterialTheme.shapes.medium,
          ),
      elevation = CardDefaults.Elevation,
  ) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
      Text(
          modifier = Modifier.padding(MaterialTheme.keylines.content),
          text = "Behavior Tweaks",
          style =
              MaterialTheme.typography.h6.copy(
                  fontWeight = FontWeight.W700,
                  color = MaterialTheme.colors.primary.copy(alpha = highAlpha),
              ),
      )

      Text(
          modifier =
              Modifier.fillMaxWidth()
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

      ToggleSwitch(
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
}
