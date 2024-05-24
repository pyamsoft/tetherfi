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

package com.pyamsoft.tetherfi.status

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.tetherfi.server.ServerNetworkBand
import com.pyamsoft.tetherfi.server.status.RunningStatus
import com.pyamsoft.tetherfi.status.sections.broadcast.renderBroadcastFrequency
import com.pyamsoft.tetherfi.status.sections.network.renderNetworkInformation
import com.pyamsoft.tetherfi.status.sections.operating.renderOperatingSettings
import com.pyamsoft.tetherfi.status.sections.performance.renderPerformanceSettings
import com.pyamsoft.tetherfi.status.sections.tweaks.renderTweaks
import com.pyamsoft.tetherfi.ui.ServerViewState
import com.pyamsoft.tetherfi.ui.renderLinks

private enum class StatusLoadedContentTypes {
  SPACER,
  BOTTOM_SPACER,
}

internal fun LazyListScope.renderLoadedContent(
    itemModifier: Modifier = Modifier,
    appName: String,
    state: StatusViewState,
    serverViewState: ServerViewState,
    isEditable: Boolean,

    // Network
    wiDiStatus: RunningStatus,
    proxyStatus: RunningStatus,
    onSsidChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onPortChanged: (String) -> Unit,
    onSelectBand: (ServerNetworkBand) -> Unit,

    // Battery
    onOpenBatterySettings: () -> Unit,

    // Notification
    showNotificationSettings: Boolean,
    onRequestNotificationPermission: () -> Unit,

    // Status button
    onShowQRCode: () -> Unit,
    onRefreshConnection: () -> Unit,

    // Wakelocks
    onToggleKeepWakeLock: () -> Unit,
    onToggleKeepWifiLock: () -> Unit,

    // Errors
    onShowNetworkError: () -> Unit,
    onShowHotspotError: () -> Unit,

    // Tweaks
    onToggleIgnoreVpn: () -> Unit,
    onToggleShutdownWithNoClients: () -> Unit,
    onToggleSocketTimeout: () -> Unit,

    // Jump links
    onJumpToHowTo: () -> Unit,

    // Power Balance
    onShowPowerBalance: () -> Unit,
) {
  renderNetworkInformation(
      itemModifier = itemModifier,
      isEditable = isEditable,
      appName = appName,
      state = state,
      serverViewState = serverViewState,
      wiDiStatus = wiDiStatus,
      proxyStatus = proxyStatus,
      onSsidChanged = onSsidChanged,
      onPasswordChanged = onPasswordChanged,
      onPortChanged = onPortChanged,
      onTogglePasswordVisibility = onTogglePasswordVisibility,
      onShowQRCode = onShowQRCode,
      onRefreshConnection = onRefreshConnection,
      onShowNetworkError = onShowNetworkError,
      onShowHotspotError = onShowHotspotError,
      onJumpToHowTo = onJumpToHowTo,
  )

  renderOperatingSettings(
      itemModifier = itemModifier,
      isEditable = isEditable,
      appName = appName,
      state = state,
      showNotificationSettings = showNotificationSettings,
      onDisableBatteryOptimizations = onOpenBatterySettings,
      onNotificationPermissionRequest = onRequestNotificationPermission,
  )

  item(
      contentType = StatusLoadedContentTypes.SPACER,
  ) {
    Spacer(
        modifier = itemModifier.height(MaterialTheme.keylines.baseline),
    )
  }

  renderBroadcastFrequency(
      itemModifier = itemModifier,
      isEditable = isEditable,
      state = state,
      onSelectBand = onSelectBand,
  )

  item(
      contentType = StatusLoadedContentTypes.SPACER,
  ) {
    Spacer(
        modifier = itemModifier.height(MaterialTheme.keylines.baseline),
    )
  }

  renderPerformanceSettings(
      itemModifier = itemModifier,
      isEditable = isEditable,
      appName = appName,
      state = state,
      onToggleKeepWakeLock = onToggleKeepWakeLock,
      onToggleKeepWifiLock = onToggleKeepWifiLock,
      onShowPowerBalance = onShowPowerBalance,
  )

  renderTweaks(
      itemModifier = itemModifier,
      isEditable = isEditable,
      appName = appName,
      state = state,
      onToggleIgnoreVpn = onToggleIgnoreVpn,
      onToggleShutdownWithNoClients = onToggleShutdownWithNoClients,
      onToggleSocketTimeout = onToggleSocketTimeout,
  )

  item(
      contentType = StatusLoadedContentTypes.SPACER,
  ) {
    Spacer(
        modifier = itemModifier.height(MaterialTheme.keylines.baseline),
    )
  }

  renderLinks(
      modifier = itemModifier,
      appName = appName,
  )

  item(
      contentType = StatusLoadedContentTypes.BOTTOM_SPACER,
  ) {
    Spacer(
        modifier =
            itemModifier.padding(top = MaterialTheme.keylines.content).navigationBarsPadding(),
    )
  }
}
