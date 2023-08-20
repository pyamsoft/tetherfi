package com.pyamsoft.tetherfi.status

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.Modifier
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.tetherfi.server.ServerNetworkBand
import com.pyamsoft.tetherfi.server.status.RunningStatus
import com.pyamsoft.tetherfi.status.sections.renderNotificationSettings
import com.pyamsoft.tetherfi.status.sections.renderPerformance
import com.pyamsoft.tetherfi.status.sections.renderTweaks
import com.pyamsoft.tetherfi.ui.LANDSCAPE_MAX_WIDTH
import com.pyamsoft.tetherfi.ui.ServerViewState
import com.pyamsoft.tetherfi.ui.renderLinks

internal fun LazyListScope.renderLoadedContent(
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

    // Jump links
    onJumpToHowTo: () -> Unit,
) {
  renderNetworkInformation(
      itemModifier = Modifier.width(LANDSCAPE_MAX_WIDTH),
      isEditable = isEditable,
      appName = appName,
      state = state,
      serverViewState = serverViewState,
      wiDiStatus = wiDiStatus,
      proxyStatus = proxyStatus,
      onSsidChanged = onSsidChanged,
      onPasswordChanged = onPasswordChanged,
      onPortChanged = onPortChanged,
      onSelectBand = onSelectBand,
      onTogglePasswordVisibility = onTogglePasswordVisibility,
      onShowQRCode = onShowQRCode,
      onRefreshConnection = onRefreshConnection,
      onShowNetworkError = onShowNetworkError,
      onShowHotspotError = onShowHotspotError,
      onDisableBatteryOptimizations = onOpenBatterySettings,
      onJumpToHowTo = onJumpToHowTo,
  )

  item(
      contentType = StatusScreenContentTypes.SPACER,
  ) {
    Spacer(
        modifier = Modifier.fillMaxWidth().height(MaterialTheme.keylines.baseline),
    )
  }

  renderPerformance(
      itemModifier = Modifier.width(LANDSCAPE_MAX_WIDTH),
      isEditable = isEditable,
      appName = appName,
      state = state,
      onToggleKeepWakeLock = onToggleKeepWakeLock,
      onToggleKeepWifiLock = onToggleKeepWifiLock,
  )

  item(
      contentType = StatusScreenContentTypes.SPACER,
  ) {
    Spacer(
        modifier = Modifier.fillMaxWidth().height(MaterialTheme.keylines.baseline),
    )
  }

  renderTweaks(
      itemModifier = Modifier.width(LANDSCAPE_MAX_WIDTH),
      isEditable = isEditable,
      appName = appName,
      state = state,
      onToggleIgnoreVpn = onToggleIgnoreVpn,
  )

  item(
      contentType = StatusScreenContentTypes.SPACER,
  ) {
    Spacer(
        modifier = Modifier.fillMaxWidth().height(MaterialTheme.keylines.baseline),
    )
  }

  if (showNotificationSettings) {
    renderNotificationSettings(
        itemModifier = Modifier.widthIn(max = LANDSCAPE_MAX_WIDTH),
        state = state,
        isEditable = isEditable,
        onRequest = onRequestNotificationPermission,
    )

    item(
        contentType = StatusScreenContentTypes.SPACER,
    ) {
      Spacer(
          modifier = Modifier.fillMaxWidth().height(MaterialTheme.keylines.content),
      )
    }
  }

  renderLinks(
      modifier = Modifier.widthIn(max = LANDSCAPE_MAX_WIDTH),
      appName = appName,
  )

  item(
      contentType = StatusScreenContentTypes.BOTTOM_SPACER,
  ) {
    Spacer(
        modifier = Modifier.padding(top = MaterialTheme.keylines.content).navigationBarsPadding(),
    )
  }
}
