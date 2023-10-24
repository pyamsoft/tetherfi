package com.pyamsoft.tetherfi.status

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pyamsoft.pydroid.core.cast
import com.pyamsoft.pydroid.ui.util.collectAsStateListWithLifecycle
import com.pyamsoft.pydroid.ui.util.fillUpToPortraitHeight
import com.pyamsoft.tetherfi.server.broadcast.BroadcastNetworkStatus
import com.pyamsoft.tetherfi.server.status.RunningStatus
import com.pyamsoft.tetherfi.service.prereq.HotspotStartBlocker
import com.pyamsoft.tetherfi.status.blockers.PermissionBlocker
import com.pyamsoft.tetherfi.status.blockers.VpnBlocker
import com.pyamsoft.tetherfi.status.trouble.TroubleshootDialog
import com.pyamsoft.tetherfi.ui.LANDSCAPE_MAX_WIDTH
import com.pyamsoft.tetherfi.ui.ServerErrorDialog
import com.pyamsoft.tetherfi.ui.ServerViewState

@Composable
internal fun StatusDialogs(
    state: StatusViewState,
    serverViewState: ServerViewState,
    appName: String,
    onDismissBlocker: (HotspotStartBlocker) -> Unit,

    // Location Permission
    onOpenPermissionSettings: () -> Unit,
    onRequestPermissions: () -> Unit,

    // Errors
    onHideSetupError: () -> Unit,
    onHideNetworkError: () -> Unit,
    onHideHotspotError: () -> Unit,
    onHideBroadcastError: () -> Unit,
    onHideProxyError: () -> Unit,
) {
  val blockers = state.startBlockers.collectAsStateListWithLifecycle()

  val isShowingHotspotError by state.isShowingHotspotError.collectAsStateWithLifecycle()
  val group by serverViewState.group.collectAsStateWithLifecycle()

  val isShowingNetworkError by state.isShowingNetworkError.collectAsStateWithLifecycle()
  val connection by serverViewState.connection.collectAsStateWithLifecycle()

  val isShowingBroadcastError by state.isShowingBroadcastError.collectAsStateWithLifecycle()
  val isShowingProxyError by state.isShowingProxyError.collectAsStateWithLifecycle()
  val wiDiStatus by state.wiDiStatus.collectAsStateWithLifecycle()
  val proxyStatus by state.proxyStatus.collectAsStateWithLifecycle()

  val isShowingSetupError by state.isShowingSetupError.collectAsStateWithLifecycle()

  // Show the Required blocks first, and if all required ones are done, show the "skippable" ones
  // even though we don't support skipping yet.
  val requiredBlockers = remember(blockers) { blockers.filter { it.required } }
  val skippableBlockers = remember(blockers) { blockers.filterNot { it.required } }

  if (requiredBlockers.isNotEmpty()) {
    for (blocker in requiredBlockers) {
      AnimatedVisibility(
          visible = blocker == HotspotStartBlocker.PERMISSION,
      ) {
        PermissionBlocker(
            modifier = Modifier.fillUpToPortraitHeight().widthIn(max = LANDSCAPE_MAX_WIDTH),
            appName = appName,
            onDismiss = { onDismissBlocker(blocker) },
            onOpenPermissionSettings = onOpenPermissionSettings,
            onRequestPermissions = onRequestPermissions,
        )
      }
    }
  } else {
    for (blocker in skippableBlockers) {
      AnimatedVisibility(
          visible = blocker == HotspotStartBlocker.VPN,
      ) {
        VpnBlocker(
            modifier = Modifier.fillUpToPortraitHeight().widthIn(max = LANDSCAPE_MAX_WIDTH),
            appName = appName,
            onDismiss = { onDismissBlocker(blocker) },
        )
      }
    }
  }

  AnimatedVisibility(
      visible = isShowingSetupError,
  ) {
    val isBroadcastError = remember(wiDiStatus) { wiDiStatus is RunningStatus.Error }
    val isProxyError = remember(proxyStatus) { proxyStatus is RunningStatus.Error }

    TroubleshootDialog(
        modifier = Modifier.fillUpToPortraitHeight().widthIn(max = LANDSCAPE_MAX_WIDTH),
        appName = appName,
        isBroadcastError = isBroadcastError,
        isProxyError = isProxyError,
        onDismiss = onHideSetupError,
    )
  }

  group.cast<BroadcastNetworkStatus.GroupInfo.Error>()?.also { err ->
    AnimatedVisibility(
        visible = isShowingHotspotError,
    ) {
      ServerErrorDialog(
          modifier = Modifier.fillUpToPortraitHeight().widthIn(max = LANDSCAPE_MAX_WIDTH),
          title = "Hotspot Initialization Error",
          error = err.error,
          onDismiss = onHideHotspotError,
      )
    }
  }

  connection.cast<BroadcastNetworkStatus.ConnectionInfo.Error>()?.also { err ->
    AnimatedVisibility(
        visible = isShowingNetworkError,
    ) {
      ServerErrorDialog(
          modifier = Modifier.fillUpToPortraitHeight().widthIn(max = LANDSCAPE_MAX_WIDTH),
          title = "Network Initialization Error",
          error = err.error,
          onDismiss = onHideNetworkError,
      )
    }
  }

  wiDiStatus.cast<RunningStatus.Error>()?.also { err ->
    AnimatedVisibility(
        visible = isShowingBroadcastError,
    ) {
      ServerErrorDialog(
          modifier = Modifier.fillUpToPortraitHeight().widthIn(max = LANDSCAPE_MAX_WIDTH),
          title = "Broadcast Initialization Error",
          error = err.throwable,
          onDismiss = onHideBroadcastError,
      )
    }
  }

  proxyStatus.cast<RunningStatus.Error>()?.also { err ->
    AnimatedVisibility(
        visible = isShowingProxyError,
    ) {
      ServerErrorDialog(
          modifier = Modifier.fillUpToPortraitHeight().widthIn(max = LANDSCAPE_MAX_WIDTH),
          title = "Proxy Initialization Error",
          error = err.throwable,
          onDismiss = onHideProxyError,
      )
    }
  }
}
