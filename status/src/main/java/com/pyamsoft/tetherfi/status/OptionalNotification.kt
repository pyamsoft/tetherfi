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

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.pydroid.ui.widget.MaterialCheckable
import com.pyamsoft.tetherfi.ui.Label

internal fun LazyListScope.renderNotificationSettings(
    itemModifier: Modifier = Modifier,
    state: StatusViewState,
    onRequest: () -> Unit,
) {
  item {
    Label(
        modifier =
            itemModifier
                .padding(horizontal = MaterialTheme.keylines.content)
                .padding(top = MaterialTheme.keylines.content)
                .padding(bottom = MaterialTheme.keylines.baseline),
        text = "Notifications",
    )
  }

  item {
    val hasPermission by state.hasNotificationPermission.collectAsState()

    MaterialCheckable(
        modifier = itemModifier.padding(horizontal = MaterialTheme.keylines.content),
        isEditable = !hasPermission,
        condition = hasPermission,
        title = "Show Hotspot Notification",
        description =
            """Keep the Hotspot alive on newer Android versions.
            |
            |Without a notification, the Hotspot may be stopped randomly."""
                .trimMargin(),
        onClick = {
          if (!hasPermission) {
            onRequest()
          }
        },
    )
  }
}
