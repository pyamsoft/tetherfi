package com.pyamsoft.tetherfi.status

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.Modifier
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.tetherfi.core.FeatureFlags
import com.pyamsoft.tetherfi.server.ServerNetworkBand
import com.pyamsoft.tetherfi.server.status.RunningStatus
import com.pyamsoft.tetherfi.status.sections.broadcast.renderBroadcastFrequency
import com.pyamsoft.tetherfi.status.sections.network.renderNetworkInformation
import com.pyamsoft.tetherfi.status.sections.notifications.renderNotificationSettings
import com.pyamsoft.tetherfi.status.sections.operating.renderOperatingSettings
import com.pyamsoft.tetherfi.status.sections.performance.renderPerformance
import com.pyamsoft.tetherfi.status.sections.tweaks.renderTweaks
import com.pyamsoft.tetherfi.ui.ServerViewState
import com.pyamsoft.tetherfi.ui.renderLinks

private enum class StatusLoadedContentTypes {
  SPACER,
  BOTTOM_SPACER,
}

internal fun LazyListScope.renderLoadedContent(
    itemModifier: Modifier = Modifier,
    featureFlags: FeatureFlags,
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
    onToggleBindProxyAll: () -> Unit,

    // Jump links
    onJumpToHowTo: () -> Unit,
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
      featureFlags = featureFlags,
      onDisableBatteryOptimizations = onOpenBatterySettings,
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

  renderPerformance(
      itemModifier = itemModifier,
      isEditable = isEditable,
      appName = appName,
      state = state,
      onToggleKeepWakeLock = onToggleKeepWakeLock,
      onToggleKeepWifiLock = onToggleKeepWifiLock,
  )

  item(
      contentType = StatusLoadedContentTypes.SPACER,
  ) {
    Spacer(
        modifier = itemModifier.height(MaterialTheme.keylines.baseline),
    )
  }

  renderTweaks(
      itemModifier = itemModifier,
      isEditable = isEditable,
      appName = appName,
      state = state,
      onToggleIgnoreVpn = onToggleIgnoreVpn,
      onToggleShutdownWithNoClients = onToggleShutdownWithNoClients,
      onToggleBindProxyAll = onToggleBindProxyAll,
  )

  item(
      contentType = StatusLoadedContentTypes.SPACER,
  ) {
    Spacer(
        modifier = itemModifier.height(MaterialTheme.keylines.baseline),
    )
  }

  if (showNotificationSettings) {
    renderNotificationSettings(
        itemModifier = itemModifier,
        state = state,
        isEditable = isEditable,
        onRequest = onRequestNotificationPermission,
    )

    item(
        contentType = StatusLoadedContentTypes.SPACER,
    ) {
      Spacer(
          modifier = itemModifier.height(MaterialTheme.keylines.content),
      )
    }
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
