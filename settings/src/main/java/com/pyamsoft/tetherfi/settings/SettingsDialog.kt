package com.pyamsoft.tetherfi.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material.Checkbox
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.pydroid.ui.app.rememberDialogProperties
import com.pyamsoft.pydroid.ui.defaults.DialogDefaults
import com.pyamsoft.pydroid.ui.settings.SettingsPage
import com.pyamsoft.pydroid.util.isDebugMode
import com.pyamsoft.tetherfi.core.AppDevEnvironment
import com.pyamsoft.tetherfi.ui.DialogToolbar

@Composable
fun SettingsDialog(
    modifier: Modifier = Modifier,
    appEnvironment: AppDevEnvironment,
    onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  val keylines = MaterialTheme.keylines
  Dialog(
      properties = rememberDialogProperties(),
      onDismissRequest = onDismiss,
  ) {
    Column(
        modifier = modifier.padding(MaterialTheme.keylines.content),
    ) {
      DialogToolbar(
          modifier = Modifier.fillMaxWidth(),
          onClose = onDismiss,
          title = {
            Text(
                text = "Settings",
            )
          },
      )
      SettingsPage(
          modifier = Modifier.fillMaxWidth().weight(1F),
          customElevation = DialogDefaults.Elevation,
          customBottomItemMargin = MaterialTheme.keylines.baseline,
          shape =
              MaterialTheme.shapes.medium.copy(
                  topStart = ZeroCornerSize,
                  topEnd = ZeroCornerSize,
              ),
          extraDebugContent = {
            if (context.isDebugMode()) {
              renderExtraDebugContent(
                  itemModifier = Modifier.fillMaxWidth().padding(bottom = keylines.baseline),
                  appEnvironment = appEnvironment,
              )
            }
          },
      )
    }
  }
}

@Composable
private fun DebugItem(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
  Row(
      modifier = modifier,
      verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(
        modifier = Modifier.weight(1F),
    ) {
      Text(
          text = title,
          style =
              MaterialTheme.typography.body1.copy(
                  color =
                      MaterialTheme.colors.onSurface.copy(
                          alpha = ContentAlpha.high,
                      ),
              ),
      )
      Text(
          text = description,
          style =
              MaterialTheme.typography.caption.copy(
                  color =
                      MaterialTheme.colors.onSurface.copy(
                          alpha = ContentAlpha.medium,
                      ),
              ),
      )
    }
    Checkbox(
        checked = checked,
        onCheckedChange = onCheckedChange,
    )
  }
}

private fun LazyListScope.renderExtraDebugContent(
    itemModifier: Modifier = Modifier,
    appEnvironment: AppDevEnvironment,
) {
  item {
    val isGroupFakeEmpty by appEnvironment.group.isEmpty.collectAsState()
    DebugItem(
        modifier = itemModifier,
        title = "Empty Group Info",
        description = "Force the WiFi Direct server to simulate returning an empty response",
        checked = isGroupFakeEmpty,
        onCheckedChange = { appEnvironment.updateGroup(isEmpty = it) },
    )
  }

  item {
    val isGroupFakeConnected by appEnvironment.group.isConnected.collectAsState()
    DebugItem(
        modifier = itemModifier,
        title = "Connected Group Info",
        description = "Force the WiFi Direct server to simulate returning a connected response",
        checked = isGroupFakeConnected,
        onCheckedChange = { appEnvironment.updateGroup(isConnected = it) },
    )
  }

  item {
    val isGroupFakeError by appEnvironment.group.isError.collectAsState()
    DebugItem(
        modifier = itemModifier,
        title = "Error Group Info",
        description = "Force the WiFi Direct server to simulate returning an error response",
        checked = isGroupFakeError,
        onCheckedChange = { appEnvironment.updateGroup(isError = it) },
    )
  }

  item {
    val isConnectionFakeEmpty by appEnvironment.connection.isEmpty.collectAsState()
    DebugItem(
        modifier = itemModifier,
        title = "Empty Connection Info",
        description = "Force the WiFi Direct server to simulate returning an empty response",
        checked = isConnectionFakeEmpty,
        onCheckedChange = { appEnvironment.updateConnection(isEmpty = it) },
    )
  }

  item {
    val isConnectionFakeConnected by appEnvironment.connection.isConnected.collectAsState()
    DebugItem(
        modifier = itemModifier,
        title = "Connected Connection Info",
        description = "Force the WiFi Direct server to simulate returning a connected response",
        checked = isConnectionFakeConnected,
        onCheckedChange = { appEnvironment.updateConnection(isConnected = it) },
    )
  }

  item {
    val isConnectionFakeError by appEnvironment.connection.isError.collectAsState()
    DebugItem(
        modifier = itemModifier,
        title = "Error Connection Info",
        description = "Force the WiFi Direct server to simulate returning an error response",
        checked = isConnectionFakeError,
        onCheckedChange = { appEnvironment.updateConnection(isError = it) },
    )
  }

  item {
    Spacer(
        modifier = Modifier.padding(MaterialTheme.keylines.content),
    )
  }
}
