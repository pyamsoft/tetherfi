package com.pyamsoft.tetherfi.status

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.pyamsoft.pydroid.ui.util.fillUpToPortraitSize
import com.pyamsoft.tetherfi.server.status.RunningStatus
import com.pyamsoft.tetherfi.server.widi.WiDiNetworkStatus
import com.pyamsoft.tetherfi.ui.ServerErrorDialog
import com.pyamsoft.tetherfi.ui.ServerViewState

@Composable
internal fun StatusDialogs(
    state: StatusViewState,
    serverViewState: ServerViewState,
    appName: String,

    // Location Permission
    onOpenPermissionSettings: () -> Unit,
    onRequestPermissions: () -> Unit,
    onDismissPermissionExplanation: () -> Unit,

    // Errors
    onHideSetupError: () -> Unit,
    onHideNetworkError: () -> Unit,
    onHideHotspotError: () -> Unit,
) {
  val isRequestingHotspotPermissions by state.isRequestingHotspotPermissions.collectAsState()

  val isShowingHotspotError by state.isShowingHotspotError.collectAsState()
  val group by serverViewState.group.collectAsState()

  val isShowingNetworkError by state.isShowingNetworkError.collectAsState()
  val connection by serverViewState.connection.collectAsState()

  val wiDiStatus by state.wiDiStatus.collectAsState()
  val proxyStatus by state.proxyStatus.collectAsState()
  val isShowingSetupError by state.isShowingSetupError.collectAsState()
  val isWifiDirectError = remember(wiDiStatus) { wiDiStatus is RunningStatus.Error }
  val isProxyError = remember(proxyStatus) { proxyStatus is RunningStatus.Error }

  AnimatedVisibility(
      visible = isShowingSetupError,
  ) {
    TroubleshootDialog(
        modifier = Modifier.fillUpToPortraitSize(),
        appName = appName,
        isWifiDirectError = isWifiDirectError,
        isProxyError = isProxyError,
        onDismiss = onHideSetupError,
    )
  }

  AnimatedVisibility(
      visible = isRequestingHotspotPermissions,
  ) {
    PermissionExplanationDialog(
        modifier = Modifier.fillUpToPortraitSize(),
        appName = appName,
        onDismissPermissionExplanation = onDismissPermissionExplanation,
        onOpenPermissionSettings = onOpenPermissionSettings,
        onRequestPermissions = onRequestPermissions,
    )
  }

  (group as? WiDiNetworkStatus.GroupInfo.Error)?.also { err ->
    AnimatedVisibility(
        visible = isShowingHotspotError,
    ) {
      ServerErrorDialog(
          modifier = Modifier.fillUpToPortraitSize(),
          title = "Hotspot Initialization Error",
          error = err.error,
          onDismiss = onHideHotspotError,
      )
    }
  }

  (connection as? WiDiNetworkStatus.ConnectionInfo.Error)?.also { err ->
    AnimatedVisibility(
        visible = isShowingNetworkError,
    ) {
      ServerErrorDialog(
          modifier = Modifier.fillUpToPortraitSize(),
          title = "Network Initialization Error",
          error = err.error,
          onDismiss = onHideNetworkError,
      )
    }
  }
}
