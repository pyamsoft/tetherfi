/*
 * Copyright 2025 pyamsoft
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

package com.pyamsoft.tetherfi.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pyamsoft.pydroid.core.cast
import com.pyamsoft.pydroid.ui.util.collectAsStateListWithLifecycle
import com.pyamsoft.tetherfi.main.blockers.LocationBlocker
import com.pyamsoft.tetherfi.main.blockers.PermissionBlocker
import com.pyamsoft.tetherfi.main.blockers.VpnBlocker
import com.pyamsoft.tetherfi.server.broadcast.BroadcastNetworkStatus
import com.pyamsoft.tetherfi.server.status.RunningStatus
import com.pyamsoft.tetherfi.service.prereq.HotspotStartBlocker
import com.pyamsoft.tetherfi.ui.dialog.ServerErrorDialog
import com.pyamsoft.tetherfi.ui.trouble.TroubleshootDialog

@Composable
fun MainDialogs(
    dialogModifier: Modifier = Modifier,
    state: MainViewState,
    appName: String,
    onDismissBlocker: (HotspotStartBlocker) -> Unit,

    // Permission
    onOpenPermissionSettings: () -> Unit,
    onRequestPermissions: () -> Unit,
    onOpenLocationSettings: () -> Unit,

    // Errors
    onDismissSetupError: () -> Unit,
    onHideNetworkError: () -> Unit,
    onHideHotspotError: () -> Unit,
    onHideBroadcastError: () -> Unit,
    onHideProxyError: () -> Unit,
) {
  val blockers = state.startBlockers.collectAsStateListWithLifecycle()
  val isShowingHotspotError by state.isShowingHotspotError.collectAsStateWithLifecycle()
  val isShowingNetworkError by state.isShowingNetworkError.collectAsStateWithLifecycle()
  val isShowingBroadcastError by state.isShowingBroadcastError.collectAsStateWithLifecycle()
  val isShowingProxyError by state.isShowingProxyError.collectAsStateWithLifecycle()
  val wiDiStatus by state.wiDiStatus.collectAsStateWithLifecycle()
  val proxyStatus by state.proxyStatus.collectAsStateWithLifecycle()
  val isShowingSetupError by state.isShowingSetupError.collectAsStateWithLifecycle()

  val group by state.group.collectAsStateWithLifecycle()
  val connection by state.connection.collectAsStateWithLifecycle()

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
    val broadcastType by state.broadcastType.collectAsStateWithLifecycle()
    val isBroadcastError = remember(wiDiStatus) { wiDiStatus is RunningStatus.Error }
    val isProxyError = remember(proxyStatus) { proxyStatus is RunningStatus.Error }

    val errorThrowable =
        remember(wiDiStatus, proxyStatus) {
          wiDiStatus.also { w ->
            if (w is RunningStatus.Error) {
              return@remember w.throwable
            }
          }

          proxyStatus.also { p ->
            if (p is RunningStatus.Error) {
              return@remember p.throwable
            }
          }

          return@remember null
        }

    TroubleshootDialog(
        modifier = dialogModifier,
        appName = appName,
        broadcastType = broadcastType,
        isBroadcastError = isBroadcastError,
        isProxyError = isProxyError,
        throwable = errorThrowable,
        onDismiss = onDismissSetupError,
    )
  }

  GroupErrorDialog(
    modifier = dialogModifier,
    group =group,
    show = isShowingHotspotError,
    onDismiss = onHideHotspotError,
  )

  GroupErrorDialog(
    modifier = dialogModifier,
    group =group,
    show = isShowingHotspotError,
    onDismiss = onHideHotspotError,
  )

  ConnectionErrorDialog(
    modifier = dialogModifier,
    connection =connection,
    show = isShowingNetworkError,
    onDismiss = onHideNetworkError,
  )

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

@Composable
private fun GroupErrorDialog(
  modifier: Modifier = Modifier,
  group: BroadcastNetworkStatus.GroupInfo,
  show: Boolean,
  onDismiss: () -> Unit,
) {
  val (groupError, setGroupError) = remember { mutableStateOf<Throwable?>(null)}

  LaunchedEffect(group) {
    val g = group
    when (g) {
      is BroadcastNetworkStatus.GroupInfo.Error -> {
        setGroupError(g.error)
      }
      is BroadcastNetworkStatus.GroupInfo.Empty -> {
        setGroupError(IllegalStateException("No Wi-Fi Direct Information Available"))
      }
      else -> {
        // Not a problem
        setGroupError(null)
      }
    }
  }

  if (groupError != null) {
    AnimatedVisibility(
      visible = show,
    ) {
      ServerErrorDialog(
        modifier = modifier,
        title = "Hotspot Initialization Error",
        error = groupError,
        onDismiss = onDismiss,
      )
    }
  }
}
@Composable
private fun ConnectionErrorDialog(
  modifier: Modifier = Modifier,
  connection: BroadcastNetworkStatus.ConnectionInfo,
  show: Boolean,
  onDismiss: () -> Unit,
) {
  val (connectionError, setConnectionError) = remember { mutableStateOf<Throwable?>(null)}

  LaunchedEffect(connection) {
    val c = connection
    when (c) {
      is BroadcastNetworkStatus.ConnectionInfo.Error -> {
        setConnectionError(c.error)
      }
      is BroadcastNetworkStatus.ConnectionInfo.Empty -> {
        setConnectionError(IllegalStateException("Wi-Fi Direct did not return Connection info"))
      }
      else -> {
        // Not a problem
        setConnectionError(null)
      }
    }
  }

  if (connectionError != null) {
    AnimatedVisibility(
      visible = show,
    ) {
      ServerErrorDialog(
        modifier = modifier,
        title = "Network Initialization Error",
        error = connectionError,
        onDismiss = onDismiss,
      )
    }
  }
}
