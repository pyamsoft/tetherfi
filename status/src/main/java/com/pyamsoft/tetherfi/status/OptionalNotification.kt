package com.pyamsoft.tetherfi.status

import android.os.Build
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.Modifier
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.pydroid.ui.widget.MaterialCheckable
import com.pyamsoft.tetherfi.ui.icons.Label

internal fun LazyListScope.renderOptionalNotification(
    itemModifier: Modifier = Modifier,
    hasPermission: Boolean,
    onRequest: () -> Unit,
) {
  if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
    return
  }

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
        description = "Show a notification when the Hotspot is active",
        onClick = {
          if (!hasPermission) {
            onRequest()
          }
        },
    )
  }

  item {
    Spacer(
        modifier =
            Modifier.fillMaxWidth()
                .padding(horizontal = MaterialTheme.keylines.content)
                .height(MaterialTheme.keylines.content),
    )
  }
}
