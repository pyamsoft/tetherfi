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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.pydroid.ui.haptics.LocalHapticManager
import com.pyamsoft.tetherfi.ui.Label
import com.pyamsoft.tetherfi.ui.checkable.CheckableCard

private enum class OptionalNotificationContentTypes {
  LABEL,
  CHECK,
}

internal fun LazyListScope.renderNotificationSettings(
    itemModifier: Modifier = Modifier,
    state: StatusViewState,
    isEditable: Boolean,
    onRequest: () -> Unit,
) {
  item(
      contentType = OptionalNotificationContentTypes.LABEL,
  ) {
    Label(
        modifier =
            itemModifier
                .padding(top = MaterialTheme.keylines.content)
                .padding(bottom = MaterialTheme.keylines.baseline),
        text = "Notifications",
    )
  }

  item(
      contentType = OptionalNotificationContentTypes.CHECK,
  ) {
    val hasPermission by state.hasNotificationPermission.collectAsStateWithLifecycle()
    val hapticManager = LocalHapticManager.current

    val canEdit = remember(isEditable, hasPermission) { isEditable && !hasPermission }

    CheckableCard(
        modifier = itemModifier,
        isEditable = canEdit,
        condition = hasPermission,
        title = "Show Hotspot Notification",
        description =
            """Keep the Hotspot alive on newer Android versions.
            |
            |Without a notification, the Hotspot may be stopped randomly."""
                .trimMargin(),
        onClick = {
          if (!hasPermission) {
            hapticManager?.actionButtonPress()
            onRequest()
          }
        },
    )
  }
}
