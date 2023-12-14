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

package com.pyamsoft.tetherfi.status.sections

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.Modifier
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.tetherfi.status.StatusViewState
import com.pyamsoft.tetherfi.ui.Label

private enum class BehaviorTweaksContentTypes {
  LABEL,
  TWEAKS,
}

internal fun LazyListScope.renderTweaks(
    itemModifier: Modifier = Modifier,
    isEditable: Boolean,
    appName: String,
    state: StatusViewState,
    onToggleIgnoreVpn: () -> Unit,
    onToggleShutdownWithNoClients: () -> Unit,
    onToggleBindProxyAll: () -> Unit,
) {
  item(
      contentType = BehaviorTweaksContentTypes.LABEL,
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
      contentType = BehaviorTweaksContentTypes.TWEAKS,
  ) {
    TweakCard(
        modifier = itemModifier,
        isEditable = isEditable,
        appName = appName,
        state = state,
        onToggleIgnoreVpn = onToggleIgnoreVpn,
        onToggleShutdownWithNoClients = onToggleShutdownWithNoClients,
        onToggleBindProxyAll = onToggleBindProxyAll,
    )
  }
}
