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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pyamsoft.pydroid.core.cast
import com.pyamsoft.pydroid.ui.util.collectAsStateListWithLifecycle
import com.pyamsoft.tetherfi.server.ServerPerformanceLimit
import com.pyamsoft.tetherfi.server.broadcast.BroadcastNetworkStatus
import com.pyamsoft.tetherfi.server.status.RunningStatus
import com.pyamsoft.tetherfi.service.prereq.HotspotStartBlocker
import com.pyamsoft.tetherfi.status.blockers.LocationBlocker
import com.pyamsoft.tetherfi.status.blockers.PermissionBlocker
import com.pyamsoft.tetherfi.status.blockers.VpnBlocker
import com.pyamsoft.tetherfi.status.sections.expert.PowerBalanceDialog
import com.pyamsoft.tetherfi.status.trouble.TroubleshootDialog
import com.pyamsoft.tetherfi.ui.ServerErrorDialog
import com.pyamsoft.tetherfi.ui.ServerViewState

@Composable
internal fun StatusDialogs(
    dialogModifier: Modifier = Modifier,
    state: StatusViewState,
    serverViewState: ServerViewState,
    appName: String,
    onDismissBlocker: (HotspotStartBlocker) -> Unit,

    // Permission
    onOpenPermissionSettings: () -> Unit,
    onRequestPermissions: () -> Unit,

    // Location
    onOpenLocationSettings: () -> Unit,

    // Errors
    onHideSetupError: () -> Unit,
    onHideNetworkError: () -> Unit,
    onHideHotspotError: () -> Unit,
    onHideBroadcastError: () -> Unit,
    onHideProxyError: () -> Unit,

    // Power Balance
    onHidePowerBalance: () -> Unit,
    onUpdatePowerBalance: (ServerPerformanceLimit) -> Unit,
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

  val isShowingPowerBalance by state.isShowingPowerBalance.collectAsStateWithLifecycle()

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
            modifier = dialogModifier,
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
            modifier = dialogModifier,
            appName = appName,
            onDismiss = { onDismissBlocker(blocker) },
        )
      }

      AnimatedVisibility(
          visible = blocker == HotspotStartBlocker.LOCATION,
      ) {
        LocationBlocker(
            modifier = dialogModifier,
            appName = appName,
            onDismiss = { onDismissBlocker(blocker) },
            onOpenLocationSettings = onOpenLocationSettings,
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
        modifier = dialogModifier,
        appName = appName,
        isBroadcastError = isBroadcastError,
        isProxyError = isProxyError,
        onDismiss = onHideSetupError,
    )
  }

  AnimatedVisibility(
      visible = isShowingPowerBalance,
  ) {
    PowerBalanceDialog(
        modifier = dialogModifier,
        state = state,
        onHidePowerBalance = onHidePowerBalance,
        onUpdatePowerBalance = onUpdatePowerBalance,
    )
  }

  group.cast<BroadcastNetworkStatus.GroupInfo.Error>()?.also { err ->
    AnimatedVisibility(
        visible = isShowingHotspotError,
    ) {
      ServerErrorDialog(
          modifier = dialogModifier,
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
          modifier = dialogModifier,
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
          modifier = dialogModifier,
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
          modifier = dialogModifier,
          title = "Proxy Initialization Error",
          error = err.throwable,
          onDismiss = onHideProxyError,
      )
    }
  }
}
