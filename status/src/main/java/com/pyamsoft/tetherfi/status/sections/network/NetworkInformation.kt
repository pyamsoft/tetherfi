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

package com.pyamsoft.tetherfi.status.sections.network

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.pydroid.ui.theme.HairlineSize
import com.pyamsoft.tetherfi.core.ExperimentalRuntimeFlags
import com.pyamsoft.tetherfi.server.status.RunningStatus
import com.pyamsoft.tetherfi.status.MutableStatusViewState
import com.pyamsoft.tetherfi.status.StatusViewState
import com.pyamsoft.tetherfi.ui.LANDSCAPE_MAX_WIDTH
import com.pyamsoft.tetherfi.ui.ServerViewState
import com.pyamsoft.tetherfi.ui.test.TEST_PASSWORD
import com.pyamsoft.tetherfi.ui.test.TEST_PORT
import com.pyamsoft.tetherfi.ui.test.TEST_SSID
import com.pyamsoft.tetherfi.ui.test.TestServerState
import com.pyamsoft.tetherfi.ui.test.makeTestRuntimeFlags
import com.pyamsoft.tetherfi.ui.test.makeTestServerState
import com.pyamsoft.tetherfi.ui.trouble.TroubleshootUnableToStart
import org.jetbrains.annotations.TestOnly

private enum class NetworkStatusWidgetsContentTypes {
  NETWORK_ERROR,
}

internal fun LazyListScope.renderNetworkInformation(
    itemModifier: Modifier = Modifier,
    appName: String,
    experimentalRuntimeFlags: ExperimentalRuntimeFlags,

    // State
    state: StatusViewState,
    serverViewState: ServerViewState,

    // Running
    isEditable: Boolean,
    wiDiStatus: RunningStatus,
    proxyStatus: RunningStatus,

    // Network config
    onSsidChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onHttpPortChanged: (String) -> Unit,
    onSocksPortChanged: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,

    // Connections
    onShowQRCode: () -> Unit,
    onRefreshConnection: () -> Unit,

    // Errors
    onShowNetworkError: () -> Unit,
    onShowHotspotError: () -> Unit,

    // Jump links
    onJumpToHowTo: () -> Unit,
    onViewSlowSpeedHelp: () -> Unit,
) {
  item(
      contentType = NetworkStatusWidgetsContentTypes.NETWORK_ERROR,
  ) {
    val broadcastType by serverViewState.broadcastType.collectAsStateWithLifecycle()
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
                      color = MaterialTheme.colorScheme.error,
                      shape = MaterialTheme.shapes.large,
                  )
                  .padding(vertical = MaterialTheme.keylines.content),
      ) {
        TroubleshootUnableToStart(
            modifier = Modifier.fillMaxWidth(),
            appName = appName,
            broadcastType = broadcastType,
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
        experimentalRuntimeFlags = experimentalRuntimeFlags,
        serverViewState = serverViewState,
        onSsidChanged = onSsidChanged,
        onPasswordChanged = onPasswordChanged,
        onHttpPortChanged = onHttpPortChanged,
        onSocksPortChanged = onSocksPortChanged,
        onTogglePasswordVisibility = onTogglePasswordVisibility,
    )
  } else {
    renderRunningItems(
        modifier = itemModifier,
        state = state,
        experimentalRuntimeFlags = experimentalRuntimeFlags,
        serverViewState = serverViewState,
        onTogglePasswordVisibility = onTogglePasswordVisibility,
        onShowQRCode = onShowQRCode,
        onRefreshConnection = onRefreshConnection,
        onShowHotspotError = onShowHotspotError,
        onShowNetworkError = onShowNetworkError,
        onJumpToHowTo = onJumpToHowTo,
        onViewSlowSpeedHelp = onViewSlowSpeedHelp,
    )
  }
}

@TestOnly
@Composable
private fun PreviewNetworkInformation(
    ssid: String = TEST_SSID,
    password: String = TEST_PASSWORD,
    port: String = "$TEST_PORT",
    server: TestServerState,
    isEditable: Boolean,
    wiDiStatus: RunningStatus,
    proxyStatus: RunningStatus,
    socks: Boolean,
) {
  LazyColumn {
    renderNetworkInformation(
        itemModifier = Modifier.width(LANDSCAPE_MAX_WIDTH),
        experimentalRuntimeFlags = makeTestRuntimeFlags(socks),
        wiDiStatus = wiDiStatus,
        proxyStatus = proxyStatus,
        appName = "TEST",
        isEditable = isEditable,
        serverViewState = makeTestServerState(server),
        state =
            MutableStatusViewState().apply {
              this.ssid.value = ssid
              this.password.value = password
              this.httpPort.value = port
            },
        onShowNetworkError = {},
        onShowQRCode = {},
        onRefreshConnection = {},
        onJumpToHowTo = {},
        onHttpPortChanged = {},
        onSocksPortChanged = {},
        onSsidChanged = {},
        onPasswordChanged = {},
        onShowHotspotError = {},
        onTogglePasswordVisibility = {},
        onViewSlowSpeedHelp = {},
    )
  }
}

@Composable
@Preview(showBackground = true)
private fun PreviewNetworkInformationBlankNoSocks() {
  PreviewNetworkInformation(
      server = TestServerState.EMPTY,
      isEditable = true,
      wiDiStatus = RunningStatus.NotRunning,
      proxyStatus = RunningStatus.NotRunning,
      ssid = "",
      password = "",
      port = "",
      socks = false,
  )
}

@Composable
@Preview(showBackground = true)
private fun PreviewNetworkInformationOnlySsidNoSocks() {
  PreviewNetworkInformation(
      server = TestServerState.EMPTY,
      isEditable = true,
      wiDiStatus = RunningStatus.NotRunning,
      proxyStatus = RunningStatus.NotRunning,
      password = "",
      port = "",
      socks = false,
  )
}

@Composable
@Preview(showBackground = true)
private fun PreviewNetworkInformationOnlyPasswordNoSocks() {
  PreviewNetworkInformation(
      server = TestServerState.EMPTY,
      isEditable = true,
      wiDiStatus = RunningStatus.NotRunning,
      proxyStatus = RunningStatus.NotRunning,
      ssid = "",
      port = "",
      socks = false,
  )
}

@Composable
@Preview(showBackground = true)
private fun PreviewNetworkInformationOnlyPortNoSocks() {
  PreviewNetworkInformation(
      server = TestServerState.EMPTY,
      isEditable = true,
      wiDiStatus = RunningStatus.NotRunning,
      proxyStatus = RunningStatus.NotRunning,
      ssid = "",
      password = "",
      socks = false,
  )
}

@Composable
@Preview(showBackground = true)
private fun PreviewNetworkInformationEmptyEditableNoSocks() {
  PreviewNetworkInformation(
      server = TestServerState.EMPTY,
      isEditable = true,
      wiDiStatus = RunningStatus.NotRunning,
      proxyStatus = RunningStatus.NotRunning,
      socks = false,
  )
}

@Composable
@Preview(showBackground = true)
private fun PreviewNetworkInformationEmptyNotEditableNoSocks() {
  PreviewNetworkInformation(
      server = TestServerState.EMPTY,
      isEditable = false,
      wiDiStatus = RunningStatus.NotRunning,
      proxyStatus = RunningStatus.NotRunning,
      socks = false,
  )
}

@Composable
@Preview(showBackground = true)
private fun PreviewNetworkInformationConnectedEditableNoSocks() {
  PreviewNetworkInformation(
      server = TestServerState.CONNECTED,
      isEditable = true,
      wiDiStatus = RunningStatus.NotRunning,
      proxyStatus = RunningStatus.NotRunning,
      socks = false,
  )
}

@Composable
@Preview(showBackground = true)
private fun PreviewNetworkInformationConnectedNotEditableNoSocks() {
  PreviewNetworkInformation(
      server = TestServerState.CONNECTED,
      isEditable = false,
      wiDiStatus = RunningStatus.NotRunning,
      proxyStatus = RunningStatus.NotRunning,
      socks = false,
  )
}

@Composable
@Preview(showBackground = true)
private fun PreviewNetworkInformationErrorEditableNoSocks() {
  PreviewNetworkInformation(
      server = TestServerState.ERROR,
      isEditable = true,
      wiDiStatus = RunningStatus.NotRunning,
      proxyStatus = RunningStatus.NotRunning,
      socks = false,
  )
}

@Composable
@Preview(showBackground = true)
private fun PreviewNetworkInformationErrorNotEditableNoSocks() {
  PreviewNetworkInformation(
      server = TestServerState.ERROR,
      isEditable = false,
      wiDiStatus = RunningStatus.NotRunning,
      proxyStatus = RunningStatus.NotRunning,
      socks = false,
  )
}

@Composable
@Preview(showBackground = true)
private fun PreviewNetworkInformationEmptyRunningNoSocks() {
  PreviewNetworkInformation(
      server = TestServerState.EMPTY,
      isEditable = false,
      wiDiStatus = RunningStatus.Running,
      proxyStatus = RunningStatus.Running,
      socks = false,
  )
}

@Composable
@Preview(showBackground = true)
private fun PreviewNetworkInformationConnectedRunningNoSocks() {
  PreviewNetworkInformation(
      server = TestServerState.CONNECTED,
      isEditable = false,
      wiDiStatus = RunningStatus.Running,
      proxyStatus = RunningStatus.Running,
      socks = false,
  )
}

@Composable
@Preview(showBackground = true)
private fun PreviewNetworkInformationErrorRunningNoSocks() {
  PreviewNetworkInformation(
      server = TestServerState.ERROR,
      isEditable = false,
      wiDiStatus = RunningStatus.Running,
      proxyStatus = RunningStatus.Running,
      socks = false,
  )
}

@Composable
@Preview(showBackground = true)
private fun PreviewNetworkInformationBlankSocks() {
  PreviewNetworkInformation(
      server = TestServerState.EMPTY,
      isEditable = true,
      wiDiStatus = RunningStatus.NotRunning,
      proxyStatus = RunningStatus.NotRunning,
      ssid = "",
      password = "",
      port = "",
      socks = true,
  )
}

@Composable
@Preview(showBackground = true)
private fun PreviewNetworkInformationOnlySsidSocks() {
  PreviewNetworkInformation(
      server = TestServerState.EMPTY,
      isEditable = true,
      wiDiStatus = RunningStatus.NotRunning,
      proxyStatus = RunningStatus.NotRunning,
      password = "",
      port = "",
      socks = true,
  )
}

@Composable
@Preview(showBackground = true)
private fun PreviewNetworkInformationOnlyPasswordSocks() {
  PreviewNetworkInformation(
      server = TestServerState.EMPTY,
      isEditable = true,
      wiDiStatus = RunningStatus.NotRunning,
      proxyStatus = RunningStatus.NotRunning,
      ssid = "",
      port = "",
      socks = true,
  )
}

@Composable
@Preview(showBackground = true)
private fun PreviewNetworkInformationOnlyPortSocks() {
  PreviewNetworkInformation(
      server = TestServerState.EMPTY,
      isEditable = true,
      wiDiStatus = RunningStatus.NotRunning,
      proxyStatus = RunningStatus.NotRunning,
      ssid = "",
      password = "",
      socks = true,
  )
}

@Composable
@Preview(showBackground = true)
private fun PreviewNetworkInformationEmptyEditableSocks() {
  PreviewNetworkInformation(
      server = TestServerState.EMPTY,
      isEditable = true,
      wiDiStatus = RunningStatus.NotRunning,
      proxyStatus = RunningStatus.NotRunning,
      socks = true,
  )
}

@Composable
@Preview(showBackground = true)
private fun PreviewNetworkInformationEmptyNotEditableSocks() {
  PreviewNetworkInformation(
      server = TestServerState.EMPTY,
      isEditable = false,
      wiDiStatus = RunningStatus.NotRunning,
      proxyStatus = RunningStatus.NotRunning,
      socks = true,
  )
}

@Composable
@Preview(showBackground = true)
private fun PreviewNetworkInformationConnectedEditableSocks() {
  PreviewNetworkInformation(
      server = TestServerState.CONNECTED,
      isEditable = true,
      wiDiStatus = RunningStatus.NotRunning,
      proxyStatus = RunningStatus.NotRunning,
      socks = true,
  )
}

@Composable
@Preview(showBackground = true)
private fun PreviewNetworkInformationConnectedNotEditableSocks() {
  PreviewNetworkInformation(
      server = TestServerState.CONNECTED,
      isEditable = false,
      wiDiStatus = RunningStatus.NotRunning,
      proxyStatus = RunningStatus.NotRunning,
      socks = true,
  )
}

@Composable
@Preview(showBackground = true)
private fun PreviewNetworkInformationErrorEditableSocks() {
  PreviewNetworkInformation(
      server = TestServerState.ERROR,
      isEditable = true,
      wiDiStatus = RunningStatus.NotRunning,
      proxyStatus = RunningStatus.NotRunning,
      socks = true,
  )
}

@Composable
@Preview(showBackground = true)
private fun PreviewNetworkInformationErrorNotEditableSocks() {
  PreviewNetworkInformation(
      server = TestServerState.ERROR,
      isEditable = false,
      wiDiStatus = RunningStatus.NotRunning,
      proxyStatus = RunningStatus.NotRunning,
      socks = true,
  )
}

@Composable
@Preview(showBackground = true)
private fun PreviewNetworkInformationEmptyRunningSocks() {
  PreviewNetworkInformation(
      server = TestServerState.EMPTY,
      isEditable = false,
      wiDiStatus = RunningStatus.Running,
      proxyStatus = RunningStatus.Running,
      socks = true,
  )
}

@Composable
@Preview(showBackground = true)
private fun PreviewNetworkInformationConnectedRunningSocks() {
  PreviewNetworkInformation(
      server = TestServerState.CONNECTED,
      isEditable = false,
      wiDiStatus = RunningStatus.Running,
      proxyStatus = RunningStatus.Running,
      socks = true,
  )
}

@Composable
@Preview(showBackground = true)
private fun PreviewNetworkInformationErrorRunningSocks() {
  PreviewNetworkInformation(
      server = TestServerState.ERROR,
      isEditable = false,
      wiDiStatus = RunningStatus.Running,
      proxyStatus = RunningStatus.Running,
      socks = true,
  )
}
