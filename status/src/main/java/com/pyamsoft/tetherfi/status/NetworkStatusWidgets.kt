package com.pyamsoft.tetherfi.status

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.pydroid.ui.theme.HairlineSize
import com.pyamsoft.tetherfi.server.ServerNetworkBand
import com.pyamsoft.tetherfi.server.status.RunningStatus
import com.pyamsoft.tetherfi.status.sections.renderBattery
import com.pyamsoft.tetherfi.status.trouble.TroubleshootUnableToStart
import com.pyamsoft.tetherfi.ui.ServerViewState

internal fun LazyListScope.renderNetworkInformation(
    itemModifier: Modifier = Modifier,
    state: StatusViewState,
    serverViewState: ServerViewState,
    appName: String,

    // Running
    isEditable: Boolean,
    wiDiStatus: RunningStatus,
    proxyStatus: RunningStatus,

    // Network config
    onSsidChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onPortChanged: (String) -> Unit,
    onSelectBand: (ServerNetworkBand) -> Unit,
    onTogglePasswordVisibility: () -> Unit,

    // Battery optimization
    onDisableBatteryOptimizations: () -> Unit,

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
      contentType = StatusScreenContentTypes.NETWORK_ERROR,
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

  renderBattery(
      itemModifier = itemModifier,
      isEditable = isEditable,
      appName = appName,
      state = state,
      onDisableBatteryOptimizations = onDisableBatteryOptimizations,
  )

  item(
      contentType = StatusScreenContentTypes.SPACER,
  ) {
    Spacer(
        modifier = Modifier.fillMaxWidth().height(MaterialTheme.keylines.baseline),
    )
  }

  item(
      contentType = StatusScreenContentTypes.BANDS,
  ) {
    NetworkBands(
        modifier = itemModifier.padding(top = MaterialTheme.keylines.content),
        isEditable = isEditable,
        state = state,
        onSelectBand = onSelectBand,
    )
  }
}

private fun LazyListScope.renderRunningItems(
    modifier: Modifier = Modifier,
    state: StatusViewState,
    serverViewState: ServerViewState,
    onTogglePasswordVisibility: () -> Unit,
    onShowQRCode: () -> Unit,
    onRefreshConnection: () -> Unit,
    onShowHotspotError: () -> Unit,
    onShowNetworkError: () -> Unit,
    onJumpToHowTo: () -> Unit,
) {
  item(
      contentType = StatusScreenContentTypes.VIEW_HOWTO,
  ) {
    ViewInstructions(
        modifier = modifier.padding(bottom = MaterialTheme.keylines.content * 2),
        onJumpToHowTo = onJumpToHowTo,
    )
  }

  item(
      contentType = StatusScreenContentTypes.VIEW_SSID,
  ) {
    ViewSsid(
        modifier = modifier.padding(bottom = MaterialTheme.keylines.baseline),
        serverViewState = serverViewState,
    )
  }

  item(
      contentType = StatusScreenContentTypes.VIEW_PASSWD,
  ) {
    ViewPassword(
        modifier = modifier.padding(bottom = MaterialTheme.keylines.baseline),
        state = state,
        serverViewState = serverViewState,
        onTogglePasswordVisibility = onTogglePasswordVisibility,
    )
  }

  item(
      contentType = StatusScreenContentTypes.VIEW_PROXY,
  ) {
    ViewProxy(
        modifier = modifier.padding(bottom = MaterialTheme.keylines.baseline),
        serverViewState = serverViewState,
    )
  }

  item(
      contentType = StatusScreenContentTypes.TILES,
  ) {
    RunningTiles(
        modifier = modifier,
        serverViewState = serverViewState,
        onShowQRCode = onShowQRCode,
        onRefreshConnection = onRefreshConnection,
        onShowHotspotError = onShowHotspotError,
        onShowNetworkError = onShowNetworkError,
    )
  }
}

private fun LazyListScope.renderEditableItems(
    modifier: Modifier = Modifier,
    state: StatusViewState,
    onSsidChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onPortChanged: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
) {
  item(
      contentType = StatusScreenContentTypes.EDIT_SSID,
  ) {
    EditSsid(
        modifier = modifier.padding(bottom = MaterialTheme.keylines.baseline),
        state = state,
        onSsidChanged = onSsidChanged,
    )
  }

  item(
      contentType = StatusScreenContentTypes.EDIT_PASSWD,
  ) {
    EditPassword(
        modifier = modifier.padding(bottom = MaterialTheme.keylines.baseline),
        state = state,
        onTogglePasswordVisibility = onTogglePasswordVisibility,
        onPasswordChanged = onPasswordChanged,
    )
  }

  item(
      contentType = StatusScreenContentTypes.EDIT_PORT,
  ) {
    EditPort(
        modifier = modifier.padding(bottom = MaterialTheme.keylines.baseline),
        state = state,
        onPortChanged = onPortChanged,
    )
  }
}
