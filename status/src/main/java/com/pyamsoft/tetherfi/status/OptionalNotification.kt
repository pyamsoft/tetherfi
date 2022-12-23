package com.pyamsoft.tetherfi.status

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.Modifier
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.pydroid.ui.widget.MaterialCheckable
import com.pyamsoft.tetherfi.ui.icons.Label

internal fun LazyListScope.renderNotificationSettings(
    itemModifier: Modifier = Modifier,
    hasPermission: Boolean,
    onRequest: () -> Unit,
) {
  item {
    Label(
        modifier =
            itemModifier
                .padding(top = MaterialTheme.keylines.content)
                .padding(bottom = MaterialTheme.keylines.baseline),
        text = "Notifications",
    )
  }

  item {
    MaterialCheckable(
        modifier = itemModifier,
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
