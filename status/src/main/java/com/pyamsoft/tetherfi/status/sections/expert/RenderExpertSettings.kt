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

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.tetherfi.server.broadcast.BroadcastType
import com.pyamsoft.tetherfi.ui.ServerViewState

private enum class ExpertSettingsContentTypes {
  SETTINGS,
}

internal fun LazyListScope.renderExpertSettings(
    itemModifier: Modifier = Modifier,
    serverViewState: ServerViewState,
    isEditable: Boolean,
    appName: String,

    // Power Balance
    onShowPowerBalance: () -> Unit,

    // Broadcast type
    onSelectBroadcastType: (BroadcastType) -> Unit,
) {
  item(
      contentType = ExpertSettingsContentTypes.SETTINGS,
  ) {
    ExpertSettings(
        modifier = itemModifier.padding(bottom = MaterialTheme.keylines.content),
        serverViewState = serverViewState,
        isEditable = isEditable,
        appName = appName,
        onShowPowerBalance = onShowPowerBalance,
        onSelectBroadcastType = onSelectBroadcastType,
    )
  }
}
