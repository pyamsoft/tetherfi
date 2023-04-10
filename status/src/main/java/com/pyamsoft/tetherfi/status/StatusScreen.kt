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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.IconToggleButton
import androidx.compose.material.LocalContentColor
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.text.trimmedLength
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.pydroid.theme.success
import com.pyamsoft.pydroid.ui.defaults.CardDefaults
import com.pyamsoft.pydroid.ui.util.fullScreenDialog
import com.pyamsoft.tetherfi.server.ServerDefaults
import com.pyamsoft.tetherfi.server.ServerNetworkBand
import com.pyamsoft.tetherfi.server.status.RunningStatus
import com.pyamsoft.tetherfi.server.widi.WiDiNetworkStatus
import com.pyamsoft.tetherfi.ui.IconButtonContent
import com.pyamsoft.tetherfi.ui.Label
import com.pyamsoft.tetherfi.ui.ServerErrorDialog
import com.pyamsoft.tetherfi.ui.ServerErrorTile
import com.pyamsoft.tetherfi.ui.ServerViewState
import com.pyamsoft.tetherfi.ui.TestServerViewState
import com.pyamsoft.tetherfi.ui.icons.QrCode
import com.pyamsoft.tetherfi.ui.icons.Visibility
import com.pyamsoft.tetherfi.ui.icons.VisibilityOff
import com.pyamsoft.tetherfi.ui.rememberServerIp
import com.pyamsoft.tetherfi.ui.rememberServerPassword
import com.pyamsoft.tetherfi.ui.rememberServerSSID
import com.pyamsoft.tetherfi.ui.renderPYDroidExtras
import kotlinx.coroutines.delay

private const val SYSTEM_DEFINED = "SYSTEM DEFINED: CANNOT CHANGE"
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
        wiDiStatus is RunningStatus.Running ||
            wiDiStatus is RunningStatus.NotRunning ||
            wiDiStatus is RunningStatus.Error
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

      item {
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

      item {
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
          item {
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

    Dialogs(
        state = state,
        serverViewState = serverViewState,
        appName = appName,
        onDismissPermissionExplanation = onDismissPermissionExplanation,
        onOpenPermissionSettings = onOpenPermissionSettings,
        onRequestPermissions = onRequestPermissions,
        onHideNetworkError = onHideNetworkError,
        onHideHotspotError = onHideHotspotError,
    )
  }
}

@Composable
private fun Dialogs(
    state: StatusViewState,
    serverViewState: ServerViewState,
    appName: String,

    // Location Permission
    onOpenPermissionSettings: () -> Unit,
    onRequestPermissions: () -> Unit,
    onDismissPermissionExplanation: () -> Unit,

    // Errors
    onHideNetworkError: () -> Unit,
    onHideHotspotError: () -> Unit,
) {
  val explainPermissions by state.explainPermissions.collectAsState()

  val isShowingHotspotError by state.isShowingHotspotError.collectAsState()
  val group by serverViewState.group.collectAsState()

  val isShowingNetworkError by state.isShowingNetworkError.collectAsState()
  val connection by serverViewState.connection.collectAsState()

  AnimatedVisibility(
      visible = explainPermissions,
  ) {
    PermissionExplanationDialog(
        modifier = Modifier.fullScreenDialog(),
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
          modifier = Modifier.fullScreenDialog(),
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
          modifier = Modifier.fullScreenDialog(),
          title = "Network Initialization Error",
          error = err.error,
          onDismiss = onHideNetworkError,
      )
    }
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

private fun LazyListScope.renderLoadedContent(
    appName: String,
    state: StatusViewState,
    serverViewState: ServerViewState,
    isEditable: Boolean,

    // Network
    wiDiStatus: RunningStatus,
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
) {
  renderNetworkInformation(
      itemModifier = Modifier.fillMaxWidth(),
      isEditable = isEditable,
      appName = appName,
      state = state,
      serverViewState = serverViewState,
      wiDiStatus = wiDiStatus,
      onSsidChanged = onSsidChanged,
      onPasswordChanged = onPasswordChanged,
      onPortChanged = onPortChanged,
      onSelectBand = onSelectBand,
      onTogglePasswordVisibility = onTogglePasswordVisibility,
      onShowQRCode = onShowQRCode,
      onRefreshConnection = onRefreshConnection,
      onShowNetworkError = onShowNetworkError,
      onShowHotspotError = onShowHotspotError,
  )

  item {
    Spacer(
        modifier =
            Modifier.fillMaxWidth()
                .padding(horizontal = MaterialTheme.keylines.content)
                .height(MaterialTheme.keylines.content),
    )
  }

  renderBatteryAndPerformance(
      itemModifier = Modifier.fillMaxWidth(),
      isEditable = isEditable,
      appName = appName,
      state = state,
      onDisableBatteryOptimizations = onOpenBatterySettings,
      onToggleKeepWakeLock = onToggleKeepWakeLock,
      onToggleKeepWifiLock = onToggleKeepWifiLock,
  )

  item {
    Spacer(
        modifier =
            Modifier.fillMaxWidth()
                .padding(horizontal = MaterialTheme.keylines.content)
                .height(MaterialTheme.keylines.content),
    )
  }

  if (showNotificationSettings) {
    renderNotificationSettings(
        itemModifier = Modifier.fillMaxWidth(),
        state = state,
        onRequest = onRequestNotificationPermission,
    )

    item {
      Spacer(
          modifier =
              Modifier.fillMaxWidth()
                  .padding(horizontal = MaterialTheme.keylines.content)
                  .height(MaterialTheme.keylines.content),
      )
    }
  }

  item {
    Spacer(
        modifier = Modifier.padding(top = MaterialTheme.keylines.content).navigationBarsPadding(),
    )
  }
}

private fun LazyListScope.renderNetworkInformation(
    itemModifier: Modifier = Modifier,
    state: StatusViewState,
    serverViewState: ServerViewState,
    appName: String,

    // Running
    isEditable: Boolean,
    wiDiStatus: RunningStatus,

    // Network config
    onSsidChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onPortChanged: (String) -> Unit,
    onSelectBand: (ServerNetworkBand) -> Unit,
    onTogglePasswordVisibility: () -> Unit,

    // Connections
    onShowQRCode: () -> Unit,
    onRefreshConnection: () -> Unit,

    // Errors
    onShowNetworkError: () -> Unit,
    onShowHotspotError: () -> Unit,
) {
  item {
    val showErrorHintMessage = remember(wiDiStatus) { wiDiStatus is RunningStatus.Error }

    AnimatedVisibility(
        visible = showErrorHintMessage,
    ) {
      Box(
          modifier =
              itemModifier
                  .padding(horizontal = MaterialTheme.keylines.content)
                  .padding(bottom = MaterialTheme.keylines.content),
      ) {
        Text(
            text =
                "Wi-Fi must be turned on for the hotspot to work. It also must not be connected to any other Wi-Fi networks. Try toggling this device's Wi-Fi off and on, then try again.",
            style =
                MaterialTheme.typography.body1.copy(
                    color = MaterialTheme.colors.error,
                ),
        )
      }
    }
  }

  item {
    val requiresPermissions by state.requiresPermissions.collectAsState()

    AnimatedVisibility(
        visible = requiresPermissions,
    ) {
      Box(
          modifier =
              itemModifier
                  .padding(bottom = MaterialTheme.keylines.content)
                  .padding(horizontal = MaterialTheme.keylines.content),
      ) {
        Text(
            text = "$appName requires permissions: Click the button and grant permissions",
            style =
                MaterialTheme.typography.caption.copy(
                    color = MaterialTheme.colors.error,
                ),
        )
      }
    }
  }

  if (isEditable) {
    item {
      val canUseCustomConfig = remember { ServerDefaults.canUseCustomConfig() }
      val ssid by state.ssid.collectAsState()
      val hotspotSsid =
          remember(
              canUseCustomConfig,
              ssid,
          ) {
            if (canUseCustomConfig) ssid else SYSTEM_DEFINED
          }

      val isValid =
          remember(
              canUseCustomConfig,
              ssid,
          ) {
            if (canUseCustomConfig) ssid.isNotBlank() else true
          }

      StatusEditor(
          itemModifier
              .padding(bottom = MaterialTheme.keylines.baseline)
              .padding(horizontal = MaterialTheme.keylines.content),
          enabled = canUseCustomConfig,
          title = "HOTSPOT NAME",
          value = hotspotSsid,
          onChange = onSsidChanged,
          leadingIcon = {
            if (canUseCustomConfig) {
              val textStyle = LocalTextStyle.current
              Text(
                  modifier =
                      Modifier.padding(
                          start = MaterialTheme.keylines.content,
                          end = MaterialTheme.keylines.typography,
                      ),
                  text = remember { ServerDefaults.getSsidPrefix() },
                  style =
                      textStyle.copy(
                          fontFamily = FontFamily.Monospace,
                          color =
                              MaterialTheme.colors.onSurface.copy(
                                  alpha = ContentAlpha.disabled,
                              ),
                      ),
              )
            }
          },
          trailingIcon = {
            ValidIcon(
                isValid = isValid,
                what = "Name",
            )
          },
      )
    }

    item {
      val canUseCustomConfig = remember { ServerDefaults.canUseCustomConfig() }
      val password by state.password.collectAsState()
      val isPasswordVisible by state.isPasswordVisible.collectAsState()
      val hotspotPassword =
          remember(
              canUseCustomConfig,
              password,
          ) {
            if (canUseCustomConfig) password else SYSTEM_DEFINED
          }

      val isValid =
          remember(
              canUseCustomConfig,
              password,
          ) {
            if (canUseCustomConfig) password.trimmedLength() in 8..63 else true
          }

      StatusEditor(
          modifier =
              itemModifier
                  .padding(bottom = MaterialTheme.keylines.baseline)
                  .padding(horizontal = MaterialTheme.keylines.content),
          enabled = canUseCustomConfig,
          title = "HOTSPOT PASSWORD",
          value = hotspotPassword,
          onChange = onPasswordChanged,
          visualTransformation =
              if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
          keyboardOptions =
              KeyboardOptions(
                  keyboardType = KeyboardType.Password,
              ),
          trailingIcon = {
            Row(
                modifier = Modifier.padding(horizontal = MaterialTheme.keylines.content),
                verticalAlignment = Alignment.CenterVertically,
            ) {
              IconToggleButton(
                  checked = isPasswordVisible,
                  onCheckedChange = { onTogglePasswordVisibility() },
              ) {
                Icon(
                    imageVector =
                        if (isPasswordVisible) Icons.Filled.VisibilityOff
                        else Icons.Filled.Visibility,
                    contentDescription =
                        if (isPasswordVisible) "Password Visible" else "Password Hidden",
                )
              }

              ValidIcon(
                  isValid = isValid,
                  what = "Password",
              )
            }
          },
      )
    }

    item {
      val port by state.port.collectAsState()
      val portNumber = remember(port) { "$port" }
      val isValid = remember(port) { port in 1025..64000 }

      StatusEditor(
          modifier =
              itemModifier
                  .padding(bottom = MaterialTheme.keylines.baseline)
                  .padding(horizontal = MaterialTheme.keylines.content),
          title = "PROXY PORT",
          value = portNumber,
          onChange = onPortChanged,
          keyboardOptions =
              KeyboardOptions(
                  keyboardType = KeyboardType.Number,
              ),
          trailingIcon = {
            ValidIcon(
                isValid = isValid,
                what = "Port",
            )
          },
      )
    }
  } else {
    item {
      val group by serverViewState.group.collectAsState()
      val ssid = rememberServerSSID(group)

      Row(
          modifier =
              itemModifier
                  .padding(bottom = MaterialTheme.keylines.baseline)
                  .padding(horizontal = MaterialTheme.keylines.content),
          verticalAlignment = Alignment.CenterVertically,
      ) {
        StatusItem(
            modifier = Modifier.padding(end = MaterialTheme.keylines.content),
            title = "HOTSPOT NAME",
            value = ssid,
            valueStyle =
                MaterialTheme.typography.h6.copy(
                    fontWeight = FontWeight.W400,
                    fontFamily = FontFamily.Monospace,
                ),
        )
      }
    }

    item {
      val group by serverViewState.group.collectAsState()
      val isPasswordVisible by state.isPasswordVisible.collectAsState()
      val password = rememberServerPassword(group, isPasswordVisible)

      Row(
          modifier =
              itemModifier
                  .padding(bottom = MaterialTheme.keylines.baseline)
                  .padding(horizontal = MaterialTheme.keylines.content),
          verticalAlignment = Alignment.CenterVertically,
      ) {
        StatusItem(
            modifier = Modifier.padding(end = MaterialTheme.keylines.content),
            title = "HOTSPOT PASSWORD",
            value = password,
            valueStyle =
                MaterialTheme.typography.h6.copy(
                    fontWeight = FontWeight.W400,
                    fontFamily = FontFamily.Monospace,
                ),
        )

        if (group is WiDiNetworkStatus.GroupInfo.Connected) {
          IconToggleButton(
              checked = isPasswordVisible,
              onCheckedChange = { onTogglePasswordVisibility() },
          ) {
            Icon(
                imageVector =
                    if (isPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                contentDescription =
                    if (isPasswordVisible) "Password Visible" else "Password Hidden",
                tint = MaterialTheme.colors.primary,
            )
          }
        }
      }
    }

    item {
      val connection by serverViewState.connection.collectAsState()
      val ipAddress = rememberServerIp(connection)

      Row(
          modifier =
              itemModifier
                  .padding(bottom = MaterialTheme.keylines.baseline)
                  .padding(horizontal = MaterialTheme.keylines.content),
          verticalAlignment = Alignment.CenterVertically,
      ) {
        StatusItem(
            title = "PROXY URL/HOSTNAME",
            value = ipAddress,
            valueStyle =
                MaterialTheme.typography.h6.copy(
                    fontWeight = FontWeight.W400,
                    fontFamily = FontFamily.Monospace,
                ),
        )
      }
    }

    item {
      val port by serverViewState.port.collectAsState()
      val portNumber = remember(port) { if (port <= 1024) "INVALID PORT" else "$port" }

      StatusItem(
          modifier =
              itemModifier
                  .padding(bottom = MaterialTheme.keylines.baseline)
                  .padding(horizontal = MaterialTheme.keylines.content),
          title = "PROXY PORT",
          value = portNumber,
          valueStyle =
              MaterialTheme.typography.h6.copy(
                  fontWeight = FontWeight.W400,
                  fontFamily = FontFamily.Monospace,
              ),
      )
    }

    item {
      val group by serverViewState.group.collectAsState()
      val connection by serverViewState.connection.collectAsState()

      TileSection(
          group = group,
          connection = connection,
          onShowQRCode = onShowQRCode,
          onRefreshConnection = onRefreshConnection,
          onShowHotspotError = onShowHotspotError,
          onShowNetworkError = onShowNetworkError,
      )
    }
  }

  item {
    val band by state.band.collectAsState()

    NetworkBands(
        modifier =
            itemModifier
                .padding(top = MaterialTheme.keylines.content)
                .padding(horizontal = MaterialTheme.keylines.content),
        isEditable = isEditable,
        band = band,
        onSelectBand = onSelectBand,
    )
  }
}

@Composable
private fun ValidIcon(
    modifier: Modifier = Modifier,
    isValid: Boolean,
    what: String,
) {
  val icon = remember(isValid) { if (isValid) Icons.Filled.Check else Icons.Filled.Close }
  val description =
      remember(
          isValid,
          what,
      ) {
        "$what is ${if (isValid) "Valid" else "Invalid"}"
      }
  val tint = if (isValid) MaterialTheme.colors.success else MaterialTheme.colors.error

  Icon(
      modifier = modifier,
      imageVector = icon,
      tint = tint,
      contentDescription = description,
  )
}

@Composable
private fun TileSection(
    modifier: Modifier = Modifier,
    group: WiDiNetworkStatus.GroupInfo,
    connection: WiDiNetworkStatus.ConnectionInfo,

    // Connections
    onShowQRCode: () -> Unit,
    onRefreshConnection: () -> Unit,

    // Errors
    onShowNetworkError: () -> Unit,
    onShowHotspotError: () -> Unit,
) {
  val isQREnabled =
      remember(
          connection,
          group,
      ) {
        connection is WiDiNetworkStatus.ConnectionInfo.Connected &&
            group is WiDiNetworkStatus.GroupInfo.Connected
      }

  Column(
      modifier = modifier.fillMaxWidth().padding(MaterialTheme.keylines.content),
  ) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = MaterialTheme.keylines.content),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      Tile(
          modifier = Modifier.weight(1F),
      ) {
        AttemptRefresh(
            onClick = onRefreshConnection,
        ) { modifier, iconButton ->
          Row(
              modifier = Modifier.fillMaxWidth().then(modifier),
              verticalAlignment = Alignment.CenterVertically,
          ) {
            val color = LocalContentColor.current

            iconButton()

            Text(
                text = "Refresh Hotspot",
                style =
                    MaterialTheme.typography.caption.copy(
                        color =
                            color.copy(
                                alpha = ContentAlpha.medium,
                            ),
                    ),
            )
          }
        }
      }

      Spacer(
          modifier = Modifier.width(MaterialTheme.keylines.content),
      )

      Tile(
          modifier = Modifier.weight(1F),
          enabled = isQREnabled,
      ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(enabled = isQREnabled) { onShowQRCode() },
            verticalAlignment = Alignment.CenterVertically,
        ) {
          val color = LocalContentColor.current

          IconButton(
              onClick = { onShowQRCode() },
              enabled = isQREnabled,
          ) {
            Icon(
                imageVector = Icons.Filled.QrCode,
                contentDescription = "QR Code",
                tint =
                    MaterialTheme.colors.primary.copy(
                        alpha = if (isQREnabled) ContentAlpha.high else ContentAlpha.disabled,
                    ),
            )
          }

          Text(
              text = "View QR Code",
              style =
                  MaterialTheme.typography.caption.copy(
                      color =
                          color.copy(
                              alpha =
                                  if (isQREnabled) ContentAlpha.medium else ContentAlpha.disabled,
                          ),
                  ),
          )
        }
      }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      (connection as? WiDiNetworkStatus.ConnectionInfo.Error)?.also {
        Tile(
            modifier = Modifier.weight(1F),
            color = MaterialTheme.colors.error,
        ) {
          ServerErrorTile(
              onShowError = onShowNetworkError,
          ) { modifier, iconButton ->
            Row(
                modifier = Modifier.fillMaxWidth().then(modifier),
                verticalAlignment = Alignment.CenterVertically,
            ) {
              val color = LocalContentColor.current

              iconButton()

              Text(
                  text = "Network Error",
                  style =
                      MaterialTheme.typography.caption.copy(
                          color =
                              color.copy(
                                  alpha = ContentAlpha.medium,
                              ),
                      ),
              )
            }
          }
        }
      }

      if (connection is WiDiNetworkStatus.ConnectionInfo.Error &&
          group is WiDiNetworkStatus.GroupInfo.Error) {
        Spacer(
            modifier = Modifier.width(MaterialTheme.keylines.content),
        )
      }

      (group as? WiDiNetworkStatus.GroupInfo.Error)?.also {
        Tile(
            modifier = Modifier.weight(1F),
            color = MaterialTheme.colors.error,
        ) {
          ServerErrorTile(
              onShowError = onShowHotspotError,
          ) { modifier, iconButton ->
            Row(
                modifier = Modifier.fillMaxWidth().then(modifier),
                verticalAlignment = Alignment.CenterVertically,
            ) {
              val color = LocalContentColor.current

              iconButton()

              Text(
                  text = "Hotspot Error",
                  style =
                      MaterialTheme.typography.caption.copy(
                          color =
                              color.copy(
                                  alpha = ContentAlpha.medium,
                              ),
                      ),
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun Tile(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colors.primary,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
  Card(
      modifier =
          modifier.border(
              width = 2.dp,
              color =
                  color.copy(
                      alpha = if (enabled) ContentAlpha.medium else ContentAlpha.disabled,
                  ),
              shape = MaterialTheme.shapes.medium,
          ),
      shape = MaterialTheme.shapes.medium,
      elevation = CardDefaults.Elevation,
  ) {
    content()
  }
}

private fun LazyListScope.renderBatteryAndPerformance(
    itemModifier: Modifier = Modifier,
    isEditable: Boolean,
    appName: String,
    state: StatusViewState,

    // Battery optimization
    onDisableBatteryOptimizations: () -> Unit,

    // Wake lock
    onToggleKeepWakeLock: () -> Unit,
    onToggleKeepWifiLock: () -> Unit,
) {
  item {
    Label(
        modifier =
            itemModifier
                .padding(horizontal = MaterialTheme.keylines.content)
                .padding(top = MaterialTheme.keylines.content)
                .padding(bottom = MaterialTheme.keylines.baseline),
        text = "Battery and Performance",
    )
  }

  item {
    Wakelocks(
        modifier =
            itemModifier
                .padding(horizontal = MaterialTheme.keylines.content)
                .padding(bottom = MaterialTheme.keylines.content),
        isEditable = isEditable,
        appName = appName,
        state = state,
        onToggleKeepWakeLock = onToggleKeepWakeLock,
        onToggleKeepWifiLock = onToggleKeepWifiLock,
    )
  }

  item {
    val isBatteryOptimizationDisabled by state.isBatteryOptimizationsIgnored.collectAsState()

    BatteryOptimization(
        modifier = itemModifier.padding(horizontal = MaterialTheme.keylines.content),
        isEditable = isEditable,
        appName = appName,
        isBatteryOptimizationDisabled = isBatteryOptimizationDisabled,
        onDisableBatteryOptimizations = onDisableBatteryOptimizations,
    )
  }
}

@Composable
private fun AttemptRefresh(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: IconButtonContent,
) {
  val (fakeSpin, setFakeSpin) = remember { mutableStateOf(false) }

  val handleResetFakeSpin by rememberUpdatedState { setFakeSpin(false) }

  val handleClick by rememberUpdatedState {
    setFakeSpin(true)
    onClick()
  }

  LaunchedEffect(fakeSpin) {
    if (fakeSpin) {
      delay(1000L)
      handleResetFakeSpin()
    }
  }

  content(
      Modifier.clickable { handleClick() },
  ) {
    IconButton(
        modifier = modifier,
        onClick = { handleClick() },
    ) {
      val angle by
          rememberInfiniteTransition()
              .animateFloat(
                  initialValue = 0F,
                  targetValue = 360F,
                  animationSpec =
                      infiniteRepeatable(
                          animation =
                              tween(
                                  durationMillis = 500,
                                  easing = LinearEasing,
                              ),
                          repeatMode = RepeatMode.Restart,
                      ),
              )

      Icon(
          modifier = Modifier.graphicsLayer { rotationZ = if (fakeSpin) angle else 0F },
          imageVector = Icons.Filled.Refresh,
          contentDescription = "Refresh",
          tint = MaterialTheme.colors.primary,
      )
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
