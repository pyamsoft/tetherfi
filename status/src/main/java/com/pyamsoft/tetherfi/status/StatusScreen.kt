package com.pyamsoft.tetherfi.status

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.pyamsoft.tetherfi.server.ServerDefaults
import com.pyamsoft.tetherfi.server.ServerNetworkBand
import com.pyamsoft.tetherfi.server.status.RunningStatus
import com.pyamsoft.tetherfi.ui.Label
import com.pyamsoft.tetherfi.ui.icons.Visibility
import com.pyamsoft.tetherfi.ui.icons.VisibilityOff
import com.pyamsoft.tetherfi.ui.renderPYDroidExtras

private const val SYSTEM_DEFINED = "SYSTEM DEFINED: CANNOT CHANGE"
private val HOTSPOT_ERROR_STATUS = RunningStatus.Error("Unable to start Hotspot")

@Composable
fun StatusScreen(
    modifier: Modifier = Modifier,
    appName: String,
    state: StatusViewState,
    onToggleProxy: () -> Unit,
    onSsidChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onPortChanged: (String) -> Unit,
    onDismissPermissionExplanation: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onOpenPermissionSettings: () -> Unit,
    onRequestPermissions: () -> Unit,
    onToggleKeepWakeLock: () -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onSelectBand: (ServerNetworkBand) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onStatusUpdated: (RunningStatus) -> Unit,
    onShowQRCodeDialog: () -> Unit,
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

  val canUseCustomConfig = remember { ServerDefaults.canUseCustomConfig() }
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
    PermissionExplanationDialog(
        modifier = Modifier.padding(pv),
        state = state,
        appName = appName,
        onDismissPermissionExplanation = onDismissPermissionExplanation,
        onOpenPermissionSettings = onOpenPermissionSettings,
        onRequestPermissions = onRequestPermissions,
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
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
              isEditable = isEditable,
              wiDiStatus = wiDiStatus,
              canUseCustomConfig = canUseCustomConfig,
              showNotificationSettings = showNotificationSettings,
              onSsidChanged = onSsidChanged,
              onPasswordChanged = onPasswordChanged,
              onPortChanged = onPortChanged,
              onOpenBatterySettings = onOpenBatterySettings,
              onToggleKeepWakeLock = onToggleKeepWakeLock,
              onSelectBand = onSelectBand,
              onRequestNotificationPermission = onRequestNotificationPermission,
              onTogglePasswordVisibility = onTogglePasswordVisibility,
              onShowQRCodeDialog = onShowQRCodeDialog,
          )
        }
      }
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
    canUseCustomConfig: Boolean,
    state: StatusViewState,
    isEditable: Boolean,
    wiDiStatus: RunningStatus,
    showNotificationSettings: Boolean,
    onSsidChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onPortChanged: (String) -> Unit,
    onSelectBand: (ServerNetworkBand) -> Unit,
    onOpenBatterySettings: () -> Unit,
    onToggleKeepWakeLock: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onShowQRCodeDialog: () -> Unit,
) {
  renderNetworkInformation(
      itemModifier = Modifier.fillMaxWidth(),
      isEditable = isEditable,
      canUseCustomConfig = canUseCustomConfig,
      appName = appName,
      state = state,
      wiDiStatus = wiDiStatus,
      onSsidChanged = onSsidChanged,
      onPasswordChanged = onPasswordChanged,
      onPortChanged = onPortChanged,
      onSelectBand = onSelectBand,
      onTogglePasswordVisibility = onTogglePasswordVisibility,
      onShowQRCodeDialog = onShowQRCodeDialog,
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
      onToggleKeepWakeLock = onToggleKeepWakeLock,
      onDisableBatteryOptimizations = onOpenBatterySettings,
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
    isEditable: Boolean,
    appName: String,
    wiDiStatus: RunningStatus,
    canUseCustomConfig: Boolean,
    onTogglePasswordVisibility: () -> Unit,
    onSsidChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onPortChanged: (String) -> Unit,
    onSelectBand: (ServerNetworkBand) -> Unit,
    onShowQRCodeDialog: () -> Unit,
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
                "Wi-Fi must be turned on for the hotspot to work. Try toggling this device's Wi-Fi off and on, then try again.",
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
          title = "HOTSPOT NAME/SSID",
          value = hotspotSsid,
          onChange = onSsidChanged,
          leadingIcon = {
            if (canUseCustomConfig) {
              val textStyle = LocalTextStyle.current
              Text(
                  modifier = Modifier.padding(horizontal = MaterialTheme.keylines.baseline),
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
            if (isValid) {
              Icon(
                  imageVector = Icons.Filled.Check,
                  tint = MaterialTheme.colors.success,
                  contentDescription = "SSID is Valid",
              )
            } else {
              Icon(
                  imageVector = Icons.Filled.Close,
                  tint = MaterialTheme.colors.error,
                  contentDescription = "SSID is Invalid",
              )
            }
          },
      )
    }

    item {
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
              IconButton(
                  onClick = onTogglePasswordVisibility,
              ) {
                Icon(
                    imageVector =
                        if (isPasswordVisible) Icons.Filled.VisibilityOff
                        else Icons.Filled.Visibility,
                    contentDescription =
                        if (isPasswordVisible) "Password Visible" else "Password Hidden",
                )
              }

              if (isValid) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    tint = MaterialTheme.colors.success,
                    contentDescription = "Password is Valid",
                )
              } else {
                Icon(
                    imageVector = Icons.Filled.Close,
                    tint = MaterialTheme.colors.error,
                    contentDescription = "Password is Invalid",
                )
              }
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
            if (isValid) {
              Icon(
                  imageVector = Icons.Filled.Check,
                  tint = MaterialTheme.colors.success,
                  contentDescription = "Port is Valid",
              )
            } else {
              Icon(
                  imageVector = Icons.Filled.Close,
                  tint = MaterialTheme.colors.error,
                  contentDescription = "Port is Invalid",
              )
            }
          },
      )
    }
  } else {
    item {
      val group by state.group.collectAsState()
      val ssid = remember(group) { group?.ssid ?: "NO SSID" }

      Row(
          modifier =
              itemModifier
                  .padding(bottom = MaterialTheme.keylines.baseline)
                  .padding(horizontal = MaterialTheme.keylines.content),
          verticalAlignment = Alignment.CenterVertically,
      ) {
        StatusItem(
            modifier = Modifier.padding(end = MaterialTheme.keylines.content),
            title = "HOTSPOT NAME/SSID",
            value = ssid,
            valueStyle =
                MaterialTheme.typography.h6.copy(
                    fontWeight = FontWeight.W400,
                    fontFamily = FontFamily.Monospace,
                ),
        )

        IconButton(
            onClick = onShowQRCodeDialog,
        ) {
          Icon(
              imageVector = Icons.Filled.QrCode,
              contentDescription = "QR Code",
              tint = MaterialTheme.colors.primary,
          )
        }
      }
    }

    item {
      val group by state.group.collectAsState()
      val isPasswordVisible by state.isPasswordVisible.collectAsState()
      val password =
          remember(
              group,
              isPasswordVisible,
          ) {
            val pw = group?.password ?: "NO PASSWORD"

            // If hidden password, map each char to the password star
            return@remember if (isPasswordVisible) {
              pw
            } else {
              pw.map { '\u2022' }.joinToString("")
            }
          }

      Row(
          modifier =
              itemModifier
                  .padding(bottom = MaterialTheme.keylines.content * 2)
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

        IconButton(
            onClick = onTogglePasswordVisibility,
        ) {
          Icon(
              imageVector =
                  if (isPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
              contentDescription = if (isPasswordVisible) "Password Visible" else "Password Hidden",
              tint = MaterialTheme.colors.primary,
          )
        }
      }
    }

    item {
      val ip by state.ip.collectAsState()
      val ipAddress = remember(ip) { ip.ifBlank { "NO IP ADDRESS" } }
      StatusItem(
          modifier =
              itemModifier
                  .padding(bottom = MaterialTheme.keylines.baseline)
                  .padding(horizontal = MaterialTheme.keylines.content),
          title = "PROXY URL/HOSTNAME",
          value = ipAddress,
          valueStyle =
              MaterialTheme.typography.h6.copy(
                  fontWeight = FontWeight.W400,
                  fontFamily = FontFamily.Monospace,
              ),
      )
    }

    item {
      val port by state.port.collectAsState()
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
  }
  item {
    val band by state.band.collectAsState()

    NetworkBands(
        modifier =
            itemModifier
                .padding(top = MaterialTheme.keylines.content)
                .padding(horizontal = MaterialTheme.keylines.content),
        isEnabled = canUseCustomConfig,
        isEditable = isEditable,
        band = band,
        onSelectBand = onSelectBand,
    )
  }
}

private fun LazyListScope.renderBatteryAndPerformance(
    itemModifier: Modifier = Modifier,
    isEditable: Boolean,
    appName: String,
    state: StatusViewState,
    onToggleKeepWakeLock: () -> Unit,
    onDisableBatteryOptimizations: () -> Unit,
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
    val keepWakeLock by state.keepWakeLock.collectAsState()

    CpuWakelock(
        modifier =
            itemModifier
                .padding(horizontal = MaterialTheme.keylines.content)
                .padding(bottom = MaterialTheme.keylines.content),
        isEditable = isEditable,
        appName = appName,
        keepWakeLock = keepWakeLock,
        onToggleKeepWakeLock = onToggleKeepWakeLock,
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
      onShowQRCodeDialog = {},
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
