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

package com.pyamsoft.tetherfi.status

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.pyamsoft.tetherfi.core.ExperimentalRuntimeFlags
import com.pyamsoft.tetherfi.server.ServerNetworkBand
import com.pyamsoft.tetherfi.server.broadcast.BroadcastType
import com.pyamsoft.tetherfi.server.network.PreferredNetwork
import com.pyamsoft.tetherfi.server.status.RunningStatus
import com.pyamsoft.tetherfi.ui.LANDSCAPE_MAX_WIDTH
import com.pyamsoft.tetherfi.ui.LoadingSpinner
import com.pyamsoft.tetherfi.ui.STATIC_HOTSPOT_ERROR
import com.pyamsoft.tetherfi.ui.ServerViewState
import com.pyamsoft.tetherfi.ui.renderPYDroidExtras
import com.pyamsoft.tetherfi.ui.test.TEST_PASSWORD
import com.pyamsoft.tetherfi.ui.test.TEST_PORT
import com.pyamsoft.tetherfi.ui.test.TEST_SSID
import com.pyamsoft.tetherfi.ui.test.TestServerState
import com.pyamsoft.tetherfi.ui.test.makeTestRuntimeFlags
import com.pyamsoft.tetherfi.ui.test.makeTestServerState
import org.jetbrains.annotations.TestOnly

private enum class StatusScreenContentTypes {
  BUTTON,
  LOADING,
}

@Composable
fun StatusScreen(
    modifier: Modifier = Modifier,
    appName: String,
    state: StatusViewState,
    lazyListState: LazyListState,
    experimentalRuntimeFlags: ExperimentalRuntimeFlags,
    serverViewState: ServerViewState,

    // Proxy
    onToggleProxy: () -> Unit,
    onStatusUpdated: (RunningStatus) -> Unit,

    // Network
    onSsidChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onSelectBand: (ServerNetworkBand) -> Unit,
    onHttpEnabledChanged: (Boolean) -> Unit,
    onHttpPortChanged: (String) -> Unit,
    onSocksEnabledChanged: (Boolean) -> Unit,
    onSocksPortChanged: (String) -> Unit,
    onEnableChangeFailed: (ServerPortTypes) -> Unit,

    // Status buttons
    onShowQRCode: () -> Unit,
    onRefreshConnection: () -> Unit,

    // Proxy Behavior
    onSelectBroadcastType: (BroadcastType) -> Unit,
    onSelectPreferredNetwork: (PreferredNetwork) -> Unit,

    // Errors
    onShowNetworkError: () -> Unit,
    onShowHotspotError: () -> Unit,
    onShowBroadcastError: () -> Unit,
    onShowProxyError: () -> Unit,

    // Jump links
    onJumpToHowTo: () -> Unit,
    onViewSlowSpeedHelp: () -> Unit,
) {
  val wiDiStatus by serverViewState.wiDiStatus.collectAsStateWithLifecycle()
  val proxyStatus by serverViewState.proxyStatus.collectAsStateWithLifecycle()

  val hotspotStatus =
      remember(
          wiDiStatus,
          proxyStatus,
      ) {
        if (wiDiStatus is RunningStatus.Error || proxyStatus is RunningStatus.Error) {
          return@remember STATIC_HOTSPOT_ERROR
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

  val loadingState by state.loadingState.collectAsStateWithLifecycle()

  val handleStatusUpdated by rememberUpdatedState(onStatusUpdated)
  LaunchedEffect(hotspotStatus) { handleStatusUpdated(hotspotStatus) }

  LazyColumn(
      modifier = modifier,
      state = lazyListState,
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
          LoadingSpinner(
              modifier =
                  Modifier.widthIn(max = LANDSCAPE_MAX_WIDTH)
                      .padding(MaterialTheme.keylines.content),
          )
        }
      }
      StatusViewState.LoadingState.DONE -> {
        renderLoadedContent(
            // Not widthIn because a TextField does not take up "all" by default
            itemModifier = Modifier.width(LANDSCAPE_MAX_WIDTH),
            appName = appName,
            state = state,
            experimentalRuntimeFlags = experimentalRuntimeFlags,
            serverViewState = serverViewState,
            isEditable = isEditable,
            wiDiStatus = wiDiStatus,
            proxyStatus = proxyStatus,
            onSsidChanged = onSsidChanged,
            onPasswordChanged = onPasswordChanged,
            onHttpEnabledChanged = onHttpEnabledChanged,
            onHttpPortChanged = onHttpPortChanged,
            onSocksEnabledChanged = onSocksEnabledChanged,
            onSocksPortChanged = onSocksPortChanged,
            onSelectBand = onSelectBand,
            onTogglePasswordVisibility = onTogglePasswordVisibility,
            onShowQRCode = onShowQRCode,
            onRefreshConnection = onRefreshConnection,
            onShowHotspotError = onShowHotspotError,
            onShowNetworkError = onShowNetworkError,
            onJumpToHowTo = onJumpToHowTo,
            onViewSlowSpeedHelp = onViewSlowSpeedHelp,
            onSelectBroadcastType = onSelectBroadcastType,
            onSelectPreferredNetwork = onSelectPreferredNetwork,
            onEnableChangeFailed = onEnableChangeFailed,
        )
      }
    }
  }
}

@TestOnly
@Composable
private fun PreviewStatusScreen(
    isLoading: Boolean,
    ssid: String = TEST_SSID,
    password: String = TEST_PASSWORD,
    port: Int = TEST_PORT,
    http: Boolean,
    socks: Boolean,
) {
  StatusScreen(
      state =
          MutableStatusViewState().apply {
            loadingState.value =
                if (isLoading) StatusViewState.LoadingState.LOADING
                else StatusViewState.LoadingState.DONE
            this.ssid.value = ssid
            this.password.value = password
            this.httpPort.value = "$port"
            band.value = ServerNetworkBand.LEGACY
          },
      serverViewState = makeTestServerState(TestServerState.EMPTY, http, socks),
      lazyListState = rememberLazyListState(),
      appName = "TEST",
      experimentalRuntimeFlags = makeTestRuntimeFlags(),
      onStatusUpdated = {},
      onSelectBand = {},
      onPasswordChanged = {},
      onHttpEnabledChanged = {},
      onHttpPortChanged = {},
      onSocksEnabledChanged = {},
      onSocksPortChanged = {},
      onSsidChanged = {},
      onToggleProxy = {},
      onTogglePasswordVisibility = {},
      onShowQRCode = {},
      onRefreshConnection = {},
      onShowHotspotError = {},
      onShowNetworkError = {},
      onJumpToHowTo = {},
      onShowBroadcastError = {},
      onShowProxyError = {},
      onViewSlowSpeedHelp = {},
      onSelectBroadcastType = {},
      onSelectPreferredNetwork = {},
      onEnableChangeFailed = {},
  )
}

@Composable
@Preview(showBackground = true)
private fun PreviewStatusScreenLoadingHttp() {
  PreviewStatusScreen(
      isLoading = true,
      http = true,
      socks = false,
  )
}

@Composable
@Preview(showBackground = true)
private fun PreviewStatusScreenEditingHttp() {
  PreviewStatusScreen(
      isLoading = false,
      http = true,
      socks = false,
  )
}

@Composable
@Preview(showBackground = true)
private fun PreviewStatusScreenEditingBadSsidHttp() {
  PreviewStatusScreen(
      isLoading = false,
      ssid = "nope",
      http = true,
      socks = false,
  )
}

@Composable
@Preview(showBackground = true)
private fun PreviewStatusScreenEditingBadPasswordHttp() {
  PreviewStatusScreen(
      isLoading = false,
      password = "nope",
      http = true,
      socks = false,
  )
}

@Composable
@Preview(showBackground = true)
private fun PreviewStatusScreenEditingBadPort1Http() {
  PreviewStatusScreen(
      isLoading = false,
      port = 1,
      http = true,
      socks = false,
  )
}

@Composable
@Preview(showBackground = true)
private fun PreviewStatusScreenEditingBadPort2Http() {
  PreviewStatusScreen(
      isLoading = false,
      port = 1_000_000,
      http = true,
      socks = false,
  )
}

@Composable
@Preview(showBackground = true)
private fun PreviewStatusScreenLoadingSocks() {
  PreviewStatusScreen(
      isLoading = true,
      http = false,
      socks = true,
  )
}

@Composable
@Preview(showBackground = true)
private fun PreviewStatusScreenEditingSocks() {
  PreviewStatusScreen(
      isLoading = false,
      http = false,
      socks = true,
  )
}

@Composable
@Preview(showBackground = true)
private fun PreviewStatusScreenEditingBadSsidSocks() {
  PreviewStatusScreen(
      isLoading = false,
      ssid = "nope",
      http = false,
      socks = true,
  )
}

@Composable
@Preview(showBackground = true)
private fun PreviewStatusScreenEditingBadPasswordSocks() {
  PreviewStatusScreen(
      isLoading = false,
      password = "nope",
      http = false,
      socks = true,
  )
}

@Composable
@Preview(showBackground = true)
private fun PreviewStatusScreenEditingBadPort1Socks() {
  PreviewStatusScreen(
      isLoading = false,
      port = 1,
      http = false,
      socks = true,
  )
}

@Composable
@Preview(showBackground = true)
private fun PreviewStatusScreenEditingBadPort2Socks() {
  PreviewStatusScreen(
      isLoading = false,
      port = 1_000_000,
      http = false,
      socks = true,
  )
}

@Composable
@Preview(showBackground = true)
private fun PreviewStatusScreenLoadingBoth() {
  PreviewStatusScreen(
      isLoading = true,
      http = true,
      socks = true,
  )
}

@Composable
@Preview(showBackground = true)
private fun PreviewStatusScreenEditingBoth() {
  PreviewStatusScreen(
      isLoading = false,
      http = true,
      socks = true,
  )
}

@Composable
@Preview(showBackground = true)
private fun PreviewStatusScreenEditingBadSsidBoth() {
  PreviewStatusScreen(
      isLoading = false,
      ssid = "nope",
      http = true,
      socks = true,
  )
}

@Composable
@Preview(showBackground = true)
private fun PreviewStatusScreenEditingBadPasswordBoth() {
  PreviewStatusScreen(
      isLoading = false,
      password = "nope",
      http = true,
      socks = true,
  )
}

@Composable
@Preview(showBackground = true)
private fun PreviewStatusScreenEditingBadPort1Both() {
  PreviewStatusScreen(
      isLoading = false,
      port = 1,
      http = true,
      socks = true,
  )
}

@Composable
@Preview(showBackground = true)
private fun PreviewStatusScreenEditingBadPort2Both() {
  PreviewStatusScreen(
      isLoading = false,
      port = 1_000_000,
      http = true,
      socks = true,
  )
}
