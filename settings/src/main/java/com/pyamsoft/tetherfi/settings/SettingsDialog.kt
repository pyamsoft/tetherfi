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

package com.pyamsoft.tetherfi.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material.Checkbox
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.pydroid.ui.app.rememberDialogProperties
import com.pyamsoft.pydroid.ui.defaults.DialogDefaults
import com.pyamsoft.pydroid.ui.settings.SettingsPage
import com.pyamsoft.pydroid.util.isDebugMode
import com.pyamsoft.tetherfi.core.AppDevEnvironment
import com.pyamsoft.tetherfi.ui.DialogToolbar

private enum class SettingsContentTypes {
  DEBUG_YOLO_ERROR,
  DEBUG_BROADCAST_ERROR,
  DEBUG_PROXY_ERROR,
  DEBUG_GROUP_EMPTY,
  DEBUG_GROUP_GOOD,
  DEBUG_GROUP_ERROR,
  DEBUG_CONN_EMPTY,
  DEBUG_CONN_GOOD,
  DEBUG_CONN_ERROR,
  BOTTOM_SPACER,
}

@Composable
fun SettingsDialog(
    modifier: Modifier = Modifier,
    appEnvironment: AppDevEnvironment,
    onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  val baselinePadding = MaterialTheme.keylines.baseline
  val itemModifier =
      remember(baselinePadding) { Modifier.fillMaxWidth().padding(bottom = baselinePadding) }

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
          dialogModifier = modifier,
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
                  itemModifier = itemModifier,
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
  item(
      contentType = SettingsContentTypes.DEBUG_YOLO_ERROR,
  ) {
    val isYoloError by appEnvironment.isYoloError.collectAsStateWithLifecycle()
    DebugItem(
        modifier = itemModifier,
        title = "Force TCP Server Error (for Stubborn Proxy)",
        description = "Force simulate a TCP Server IOException, see if Stubborn Proxy works (YOLO mode)",
        checked = isYoloError,
        onCheckedChange = { appEnvironment.updateYolo(it) },
    )
  }

  item(
      contentType = SettingsContentTypes.DEBUG_BROADCAST_ERROR,
  ) {
    val isBroadcastFakeError by appEnvironment.isBroadcastFakeError.collectAsStateWithLifecycle()
    DebugItem(
        modifier = itemModifier,
        title = "Force Broadcast Error",
        description = "Force simulate a Broadcast Error",
        checked = isBroadcastFakeError,
        onCheckedChange = { appEnvironment.updateBroadcast(it) },
    )
  }

  item(
      contentType = SettingsContentTypes.DEBUG_PROXY_ERROR,
  ) {
    val isProxyFakeError by appEnvironment.isProxyFakeError.collectAsStateWithLifecycle()
    DebugItem(
        modifier = itemModifier,
        title = "Force Proxy Error",
        description = "Force simulate a Proxy Error",
        checked = isProxyFakeError,
        onCheckedChange = { appEnvironment.updateProxy(it) },
    )
  }

  item(
      contentType = SettingsContentTypes.DEBUG_GROUP_EMPTY,
  ) {
    val isGroupFakeEmpty by appEnvironment.group.isEmpty.collectAsStateWithLifecycle()
    DebugItem(
        modifier = itemModifier,
        title = "Empty Group Info",
        description = "Force the WiFi Direct server to simulate returning an empty response",
        checked = isGroupFakeEmpty,
        onCheckedChange = { appEnvironment.updateGroup(isEmpty = it) },
    )
  }

  item(
      contentType = SettingsContentTypes.DEBUG_GROUP_GOOD,
  ) {
    val isGroupFakeConnected by appEnvironment.group.isConnected.collectAsStateWithLifecycle()
    DebugItem(
        modifier = itemModifier,
        title = "Connected Group Info",
        description = "Force the WiFi Direct server to simulate returning a connected response",
        checked = isGroupFakeConnected,
        onCheckedChange = { appEnvironment.updateGroup(isConnected = it) },
    )
  }

  item(
      contentType = SettingsContentTypes.DEBUG_GROUP_ERROR,
  ) {
    val isGroupFakeError by appEnvironment.group.isError.collectAsStateWithLifecycle()
    DebugItem(
        modifier = itemModifier,
        title = "Error Group Info",
        description = "Force the WiFi Direct server to simulate returning an error response",
        checked = isGroupFakeError,
        onCheckedChange = { appEnvironment.updateGroup(isError = it) },
    )
  }

  item(
      contentType = SettingsContentTypes.DEBUG_CONN_EMPTY,
  ) {
    val isConnectionFakeEmpty by appEnvironment.connection.isEmpty.collectAsStateWithLifecycle()
    DebugItem(
        modifier = itemModifier,
        title = "Empty Connection Info",
        description = "Force the WiFi Direct server to simulate returning an empty response",
        checked = isConnectionFakeEmpty,
        onCheckedChange = { appEnvironment.updateConnection(isEmpty = it) },
    )
  }

  item(
      contentType = SettingsContentTypes.DEBUG_CONN_GOOD,
  ) {
    val isConnectionFakeConnected by
        appEnvironment.connection.isConnected.collectAsStateWithLifecycle()
    DebugItem(
        modifier = itemModifier,
        title = "Connected Connection Info",
        description = "Force the WiFi Direct server to simulate returning a connected response",
        checked = isConnectionFakeConnected,
        onCheckedChange = { appEnvironment.updateConnection(isConnected = it) },
    )
  }

  item(
      contentType = SettingsContentTypes.DEBUG_CONN_ERROR,
  ) {
    val isConnectionFakeError by appEnvironment.connection.isError.collectAsStateWithLifecycle()
    DebugItem(
        modifier = itemModifier,
        title = "Error Connection Info",
        description = "Force the WiFi Direct server to simulate returning an error response",
        checked = isConnectionFakeError,
        onCheckedChange = { appEnvironment.updateConnection(isError = it) },
    )
  }

  item(
      contentType = SettingsContentTypes.BOTTOM_SPACER,
  ) {
    Spacer(
        modifier = Modifier.padding(MaterialTheme.keylines.content),
    )
  }
}
