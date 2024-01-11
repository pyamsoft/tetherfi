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

package com.pyamsoft.tetherfi.status.sections.network

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.pydroid.ui.theme.HairlineSize
import com.pyamsoft.tetherfi.server.status.RunningStatus
import com.pyamsoft.tetherfi.status.StatusViewState
import com.pyamsoft.tetherfi.status.trouble.TroubleshootUnableToStart
import com.pyamsoft.tetherfi.ui.ServerViewState

private enum class NetworkStatusWidgetsContentTypes {
  NETWORK_ERROR,
}

internal fun LazyListScope.renderNetworkInformation(
    itemModifier: Modifier = Modifier,
    appName: String,

    // State
    state: StatusViewState,
    serverViewState: ServerViewState,

    // Running
    isEditable: Boolean,
    wiDiStatus: RunningStatus,
    proxyStatus: RunningStatus,

    // Network config
    onSsidChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onPortChanged: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,

    // Connections
    onShowQRCode: () -> Unit,
    onRefreshConnection: () -> Unit,

    // Errors
    onShowNetworkError: () -> Unit,
    onShowHotspotError: () -> Unit,

    // Jump links
    onJumpToHowTo: () -> Unit,
) {
  item(
      contentType = NetworkStatusWidgetsContentTypes.NETWORK_ERROR,
  ) {
    val isBroadcastError = remember(wiDiStatus) { wiDiStatus is RunningStatus.Error }
    val isProxyError = remember(proxyStatus) { proxyStatus is RunningStatus.Error }
    val showErrorHintMessage =
        remember(
            isBroadcastError,
            isProxyError,
        ) {
          isBroadcastError || isProxyError
        }

    AnimatedVisibility(
        visible = showErrorHintMessage,
    ) {
      Box(
          modifier =
              itemModifier
                  .padding(bottom = MaterialTheme.keylines.content * 2)
                  .border(
                      width = HairlineSize,
                      color = MaterialTheme.colors.error,
                      shape = MaterialTheme.shapes.medium,
                  )
                  .padding(vertical = MaterialTheme.keylines.content),
      ) {
        TroubleshootUnableToStart(
            modifier = Modifier.fillMaxWidth(),
            appName = appName,
            isBroadcastError = isBroadcastError,
            isProxyError = isProxyError,
        )
      }
    }
  }

  if (isEditable) {
    renderEditableItems(
        modifier = itemModifier,
        state = state,
        onSsidChanged = onSsidChanged,
        onPasswordChanged = onPasswordChanged,
        onPortChanged = onPortChanged,
        onTogglePasswordVisibility = onTogglePasswordVisibility,
    )
  } else {
    renderRunningItems(
        modifier = itemModifier,
        state = state,
        serverViewState = serverViewState,
        onTogglePasswordVisibility = onTogglePasswordVisibility,
        onShowQRCode = onShowQRCode,
        onRefreshConnection = onRefreshConnection,
        onShowHotspotError = onShowHotspotError,
        onShowNetworkError = onShowNetworkError,
        onJumpToHowTo = onJumpToHowTo,
    )
  }
}
