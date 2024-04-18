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

import android.os.Build
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.pydroid.ui.util.fillUpToPortraitSize
import com.pyamsoft.tetherfi.server.ServerNetworkBand
import com.pyamsoft.tetherfi.server.ServerPerformanceLimit
import com.pyamsoft.tetherfi.server.status.RunningStatus
import com.pyamsoft.tetherfi.service.prereq.HotspotStartBlocker
import com.pyamsoft.tetherfi.ui.LANDSCAPE_MAX_WIDTH
import com.pyamsoft.tetherfi.ui.ServerViewState
import com.pyamsoft.tetherfi.ui.TestServerViewState
import com.pyamsoft.tetherfi.ui.renderPYDroidExtras

private enum class StatusScreenContentTypes {
  BUTTON,
  LOADING,
}

private val staticHotspotError =
    RunningStatus.HotspotError(
        RuntimeException("Unable to start Hotspot"),
    )

@Composable
fun StatusScreen(
    modifier: Modifier = Modifier,
    appName: String,
    state: StatusViewState,
    serverViewState: ServerViewState,

    // Proxy
    onToggleProxy: () -> Unit,
    onStatusUpdated: (RunningStatus) -> Unit,

    // Network
    onSsidChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onSelectBand: (ServerNetworkBand) -> Unit,
    onPortChanged: (String) -> Unit,

    // Battery Optimization
    onOpenBatterySettings: () -> Unit,

    // Blockers
    onDismissBlocker: (HotspotStartBlocker) -> Unit,

    // Location Permission
    onOpenPermissionSettings: () -> Unit,
    onRequestPermissions: () -> Unit,

    // Notification
    onRequestNotificationPermission: () -> Unit,

    // Status buttons
    onShowQRCode: () -> Unit,
    onRefreshConnection: () -> Unit,

    // Wake lock
    onToggleKeepWakeLock: () -> Unit,
    onToggleKeepWifiLock: () -> Unit,

    // Errors
    onHideSetupError: () -> Unit,
    onShowNetworkError: () -> Unit,
    onHideNetworkError: () -> Unit,
    onShowHotspotError: () -> Unit,
    onHideHotspotError: () -> Unit,
    onShowBroadcastError: () -> Unit,
    onHideBroadcastError: () -> Unit,
    onShowProxyError: () -> Unit,
    onHideProxyError: () -> Unit,

    // Tweaks
    onToggleIgnoreVpn: () -> Unit,
    onToggleShutdownWithNoClients: () -> Unit,
    onToggleProxyYolo: () -> Unit,

    // Jump links
    onJumpToHowTo: () -> Unit,

    // Power Balance
    onShowPowerBalance: () -> Unit,
    onHidePowerBalance: () -> Unit,
    onUpdatePowerBalance: (ServerPerformanceLimit) -> Unit,
) {
  val wiDiStatus by state.wiDiStatus.collectAsStateWithLifecycle()
  val proxyStatus by state.proxyStatus.collectAsStateWithLifecycle()

  val hotspotStatus =
      remember(
          wiDiStatus,
          proxyStatus,
      ) {
        if (wiDiStatus is RunningStatus.Error || proxyStatus is RunningStatus.Error) {
          return@remember staticHotspotError
        }

        // If either is starting, mark us starting
        if (wiDiStatus is RunningStatus.Starting || proxyStatus is RunningStatus.Starting) {
          return@remember RunningStatus.Starting
        }

        // If the wifi direct broadcast is up, but the proxy is not up yet, mark starting
        if (wiDiStatus is RunningStatus.Running && proxyStatus !is RunningStatus.Running) {
          return@remember RunningStatus.Starting
        }

        // If either is stopping, mark us stopping
        if (wiDiStatus is RunningStatus.Stopping || proxyStatus is RunningStatus.Stopping) {
          return@remember RunningStatus.Stopping
        }

        if (wiDiStatus is RunningStatus.Running) {
          // If the Wifi Direct is running, watch the proxy status
          return@remember proxyStatus
        } else {
          // Otherwise fallback to wiDi status
          return@remember wiDiStatus
        }
      }

  val isButtonEnabled =
      remember(hotspotStatus) {
        hotspotStatus is RunningStatus.Running ||
            hotspotStatus is RunningStatus.NotRunning ||
            hotspotStatus is RunningStatus.Error
      }

  val isEditable =
      remember(hotspotStatus) {
        when (hotspotStatus) {
          is RunningStatus.Running,
          is RunningStatus.Starting,
          is RunningStatus.Stopping -> false
          else -> true
        }
      }

  val showNotificationSettings = remember { Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU }
  val loadingState by state.loadingState.collectAsStateWithLifecycle()

  val handleStatusUpdated by rememberUpdatedState(onStatusUpdated)
  LaunchedEffect(hotspotStatus) { handleStatusUpdated(hotspotStatus) }

  LazyColumn(
      modifier = modifier,
      contentPadding = PaddingValues(horizontal = MaterialTheme.keylines.content),
      horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    item(
        contentType = StatusScreenContentTypes.BUTTON,
    ) {
      HotspotStarter(
          modifier =
              Modifier.width(LANDSCAPE_MAX_WIDTH).padding(top = MaterialTheme.keylines.content),
          isButtonEnabled = isButtonEnabled,
          hotspotStatus = hotspotStatus,
          appName = appName,
          onToggleProxy = onToggleProxy,
      )
    }

    renderPYDroidExtras(
        modifier = Modifier.widthIn(max = LANDSCAPE_MAX_WIDTH),
    )

    renderHotspotStatus(
        itemModifier = Modifier.widthIn(max = LANDSCAPE_MAX_WIDTH),
        wiDiStatus = wiDiStatus,
        proxyStatus = proxyStatus,
        hotspotStatus = hotspotStatus,
        onShowBroadcastError = onShowBroadcastError,
        onShowProxyError = onShowProxyError,
    )

    when (loadingState) {
      StatusViewState.LoadingState.NONE,
      StatusViewState.LoadingState.LOADING -> {
        item(
            contentType = StatusScreenContentTypes.LOADING,
        ) {
          StatusLoading(
              modifier =
                  Modifier.widthIn(max = LANDSCAPE_MAX_WIDTH)
                      .padding(MaterialTheme.keylines.content),
          )
        }
      }
      StatusViewState.LoadingState.DONE -> {
        renderLoadedContent(
            itemModifier = Modifier.width(LANDSCAPE_MAX_WIDTH),
            appName = appName,
            state = state,
            serverViewState = serverViewState,
            isEditable = isEditable,
            wiDiStatus = wiDiStatus,
            proxyStatus = proxyStatus,
            showNotificationSettings = showNotificationSettings,
            onSsidChanged = onSsidChanged,
            onPasswordChanged = onPasswordChanged,
            onPortChanged = onPortChanged,
            onOpenBatterySettings = onOpenBatterySettings,
            onSelectBand = onSelectBand,
            onRequestNotificationPermission = onRequestNotificationPermission,
            onTogglePasswordVisibility = onTogglePasswordVisibility,
            onShowQRCode = onShowQRCode,
            onRefreshConnection = onRefreshConnection,
            onToggleKeepWakeLock = onToggleKeepWakeLock,
            onToggleKeepWifiLock = onToggleKeepWifiLock,
            onShowHotspotError = onShowHotspotError,
            onShowNetworkError = onShowNetworkError,
            onToggleIgnoreVpn = onToggleIgnoreVpn,
            onToggleShutdownWithNoClients = onToggleShutdownWithNoClients,
            onToggleProxyYolo = onToggleProxyYolo,
            onJumpToHowTo = onJumpToHowTo,
            onShowPowerBalance = onShowPowerBalance,
        )
      }
    }
  }

  StatusDialogs(
      dialogModifier = Modifier.fillUpToPortraitSize().widthIn(max = LANDSCAPE_MAX_WIDTH),
      state = state,
      serverViewState = serverViewState,
      appName = appName,
      onDismissBlocker = onDismissBlocker,
      onOpenPermissionSettings = onOpenPermissionSettings,
      onRequestPermissions = onRequestPermissions,
      onHideNetworkError = onHideNetworkError,
      onHideHotspotError = onHideHotspotError,
      onHideSetupError = onHideSetupError,
      onHideProxyError = onHideProxyError,
      onHideBroadcastError = onHideBroadcastError,
      onHidePowerBalance = onHidePowerBalance,
      onUpdatePowerBalance = onUpdatePowerBalance,
  )
}

@Composable
private fun PreviewStatusScreen(
    isLoading: Boolean,
    ssid: String = "MySsid",
    password: String = "MyPassword",
    port: Int = 8228,
) {
  StatusScreen(
      state =
          MutableStatusViewState().apply {
            loadingState.value =
                if (isLoading) StatusViewState.LoadingState.LOADING
                else StatusViewState.LoadingState.DONE
            this.ssid.value = ssid
            this.password.value = password
            this.port.value = "$port"
            band.value = ServerNetworkBand.LEGACY
          },
      serverViewState = TestServerViewState(),
      appName = "TEST",
      onStatusUpdated = {},
      onRequestNotificationPermission = {},
      onToggleKeepWakeLock = {},
      onSelectBand = {},
      onDismissBlocker = {},
      onOpenBatterySettings = {},
      onOpenPermissionSettings = {},
      onPasswordChanged = {},
      onPortChanged = {},
      onRequestPermissions = {},
      onSsidChanged = {},
      onToggleProxy = {},
      onTogglePasswordVisibility = {},
      onShowQRCode = {},
      onRefreshConnection = {},
      onToggleKeepWifiLock = {},
      onHideHotspotError = {},
      onShowHotspotError = {},
      onShowNetworkError = {},
      onHideNetworkError = {},
      onHideSetupError = {},
      onToggleIgnoreVpn = {},
      onToggleShutdownWithNoClients = {},
      onToggleProxyYolo = {},
      onJumpToHowTo = {},
      onShowBroadcastError = {},
      onHideBroadcastError = {},
      onShowProxyError = {},
      onHideProxyError = {},
      onUpdatePowerBalance = {},
      onHidePowerBalance = {},
      onShowPowerBalance = {},
  )
}

@Preview
@Composable
private fun PreviewStatusScreenLoading() {
  PreviewStatusScreen(
      isLoading = true,
  )
}

@Preview
@Composable
private fun PreviewStatusScreenEditing() {
  PreviewStatusScreen(
      isLoading = false,
  )
}

@Preview
@Composable
private fun PreviewStatusScreenEditingBadSsid() {
  PreviewStatusScreen(
      isLoading = false,
      ssid = "nope",
  )
}

@Preview
@Composable
private fun PreviewStatusScreenEditingBadPassword() {
  PreviewStatusScreen(
      isLoading = false,
      password = "nope",
  )
}

@Preview
@Composable
private fun PreviewStatusScreenEditingBadPort1() {
  PreviewStatusScreen(
      isLoading = false,
      port = 1,
  )
}

@Preview
@Composable
private fun PreviewStatusScreenEditingBadPort2() {
  PreviewStatusScreen(
      isLoading = false,
      port = 1_000_000,
  )
}
