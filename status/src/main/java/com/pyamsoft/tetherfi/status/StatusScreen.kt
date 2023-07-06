/*
 * Copyright 2023 pyamsoft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.pydroid.ui.defaults.CardDefaults
import com.pyamsoft.tetherfi.server.ServerNetworkBand
import com.pyamsoft.tetherfi.server.status.RunningStatus
import com.pyamsoft.tetherfi.ui.ServerViewState
import com.pyamsoft.tetherfi.ui.TestServerViewState
import com.pyamsoft.tetherfi.ui.renderPYDroidExtras

private val HOTSPOT_ERROR_STATUS = RunningStatus.Error("Unable to start Hotspot")

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

    // Location Permission
    onOpenPermissionSettings: () -> Unit,
    onRequestPermissions: () -> Unit,
    onDismissPermissionExplanation: () -> Unit,

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
) {
  val wiDiStatus by state.wiDiStatus.collectAsState()
  val proxyStatus by state.proxyStatus.collectAsState()

  val hotspotStatus =
      remember(
          wiDiStatus,
          proxyStatus,
      ) {
        if (wiDiStatus is RunningStatus.Error || proxyStatus is RunningStatus.Error) {
          return@remember HOTSPOT_ERROR_STATUS
        }

        // If either is starting, mark us starting
        if (wiDiStatus is RunningStatus.Starting || proxyStatus is RunningStatus.Starting) {
          return@remember RunningStatus.Starting
        }

        // If either is stopping, mark us stopping
        if (wiDiStatus is RunningStatus.Stopping || proxyStatus is RunningStatus.Stopping) {
          return@remember RunningStatus.Stopping
        }

        // If we are not running
        if (wiDiStatus is RunningStatus.NotRunning && proxyStatus is RunningStatus.NotRunning) {
          return@remember RunningStatus.NotRunning
        }

        // If we are running
        if (wiDiStatus is RunningStatus.Running && proxyStatus is RunningStatus.Running) {
          return@remember RunningStatus.Running
        }

        // Otherwise fallback to wiDi status
        return@remember wiDiStatus
      }

  val isButtonEnabled =
      remember(hotspotStatus) {
        hotspotStatus is RunningStatus.Running ||
            hotspotStatus is RunningStatus.NotRunning ||
            hotspotStatus is RunningStatus.Error
      }

  val buttonText =
      remember(hotspotStatus) {
        when (hotspotStatus) {
          is RunningStatus.Error -> "$appName Hotspot Error"
          is RunningStatus.NotRunning -> "Start $appName Hotspot"
          is RunningStatus.Running -> "Stop $appName Hotspot"
          else -> "$appName is thinking..."
        }
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
  val loadingState by state.loadingState.collectAsState()

  val handleStatusUpdated by rememberUpdatedState(onStatusUpdated)
  LaunchedEffect(hotspotStatus) { handleStatusUpdated(hotspotStatus) }

  Scaffold(
      modifier = modifier,
  ) { pv ->
    LazyColumn(
        modifier = Modifier.padding(pv).fillMaxSize(),
    ) {
      renderPYDroidExtras()

      item(
          contentType = StatusScreenContentTypes.BUTTON,
      ) {
        Button(
            modifier =
                Modifier.fillMaxWidth()
                    .padding(top = MaterialTheme.keylines.content)
                    .padding(horizontal = MaterialTheme.keylines.content),
            enabled = isButtonEnabled,
            onClick = onToggleProxy,
        ) {
          Text(
              text = buttonText,
              style =
                  MaterialTheme.typography.body1.copy(
                      fontWeight = FontWeight.W700,
                  ),
          )
        }
      }

      item(
          contentType = StatusScreenContentTypes.STATUS,
      ) {
        StatusCard(
            modifier = Modifier.fillMaxWidth().padding(vertical = MaterialTheme.keylines.content),
            wiDiStatus = wiDiStatus,
            proxyStatus = proxyStatus,
            hotspotStatus = hotspotStatus,
        )
      }

      when (loadingState) {
        StatusViewState.LoadingState.NONE,
        StatusViewState.LoadingState.LOADING -> {
          item(
              contentType = StatusScreenContentTypes.LOADING,
          ) {
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .padding(top = MaterialTheme.keylines.content)
                        .padding(horizontal = MaterialTheme.keylines.content),
                contentAlignment = Alignment.Center,
            ) {
              CircularProgressIndicator(
                  modifier = Modifier.size(120.dp),
              )
            }
          }
        }
        StatusViewState.LoadingState.DONE -> {
          renderLoadedContent(
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
          )
        }
      }
    }

    StatusDialogs(
        state = state,
        serverViewState = serverViewState,
        appName = appName,
        onDismissPermissionExplanation = onDismissPermissionExplanation,
        onOpenPermissionSettings = onOpenPermissionSettings,
        onRequestPermissions = onRequestPermissions,
        onHideNetworkError = onHideNetworkError,
        onHideHotspotError = onHideHotspotError,
        onHideSetupError = onHideSetupError,
    )
  }
}

@Composable
private fun StatusCard(
    modifier: Modifier = Modifier,
    wiDiStatus: RunningStatus,
    proxyStatus: RunningStatus,
    hotspotStatus: RunningStatus,
) {
  Card(
      modifier = modifier.padding(MaterialTheme.keylines.content),
      elevation = CardDefaults.Elevation,
  ) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(MaterialTheme.keylines.content),
    ) {
      Row(
          modifier = Modifier.fillMaxWidth().padding(bottom = MaterialTheme.keylines.content),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceEvenly,
      ) {
        DisplayStatus(
            modifier = Modifier.weight(1F, fill = false),
            title = "Broadcast Status:",
            status = wiDiStatus,
            size = StatusSize.SMALL,
        )

        DisplayStatus(
            modifier = Modifier.weight(1F, fill = false),
            title = "Proxy Status:",
            status = proxyStatus,
            size = StatusSize.SMALL,
        )
      }

      Box(
          modifier = Modifier.fillMaxWidth(),
          contentAlignment = Alignment.Center,
      ) {
        DisplayStatus(
            title = "Hotspot Status:",
            status = hotspotStatus,
            size = StatusSize.NORMAL,
        )
      }
    }
  }
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
            this.port.value = port
            band.value = ServerNetworkBand.LEGACY
          },
      serverViewState = TestServerViewState(),
      appName = "TEST",
      onStatusUpdated = {},
      onRequestNotificationPermission = {},
      onToggleKeepWakeLock = {},
      onSelectBand = {},
      onDismissPermissionExplanation = {},
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
