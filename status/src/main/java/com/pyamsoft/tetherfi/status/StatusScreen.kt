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
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.pydroid.ui.defaults.CardDefaults
import com.pyamsoft.tetherfi.server.ServerDefaults
import com.pyamsoft.tetherfi.server.ServerNetworkBand
import com.pyamsoft.tetherfi.server.status.RunningStatus
import com.pyamsoft.tetherfi.ui.Label
import com.pyamsoft.tetherfi.ui.renderPYDroidExtras

private const val SYSTEM_DEFINED = "SYSTEM DEFINED: CANNOT CHANGE"

@Composable
fun StatusScreen(
    modifier: Modifier = Modifier,
    appName: String,
    state: StatusViewState,
    hasNotificationPermission: Boolean,
    onToggle: () -> Unit,
    onSsidChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onPortChanged: (String) -> Unit,
    onDismissPermissionExplanation: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onOpenPermissionSettings: () -> Unit,
    onRequestPermissions: () -> Unit,
    onToggleKeepWakeLock: () -> Unit,
    onSelectBand: (ServerNetworkBand) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onStatusUpdated: (RunningStatus) -> Unit,
) {
  val wiDiStatus = state.wiDiStatus
  val proxyStatus = state.proxyStatus

  val hotspotStatus =
      remember(
          wiDiStatus,
          proxyStatus,
      ) {
        // If either is starting, mark us starting
        if (wiDiStatus == RunningStatus.Starting || proxyStatus == RunningStatus.Starting) {
          return@remember RunningStatus.Starting
        }

        // If either is stopping, mark us stopping
        if (wiDiStatus == RunningStatus.Stopping || proxyStatus == RunningStatus.Stopping) {
          return@remember RunningStatus.Stopping
        }

        // If we are not running
        if (wiDiStatus == RunningStatus.NotRunning && proxyStatus == RunningStatus.NotRunning) {
          return@remember RunningStatus.NotRunning
        }

        // If we are running
        if (wiDiStatus == RunningStatus.Running && proxyStatus == RunningStatus.Running) {
          return@remember RunningStatus.Running
        }

        // Otherwise fallback to wiDi status
        return@remember wiDiStatus
      }

  val handleStatusUpdated by rememberUpdatedState(onStatusUpdated)
  LaunchedEffect(hotspotStatus) { handleStatusUpdated(hotspotStatus) }

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
            onClick = onToggle,
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

      when (state.loadingState) {
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
              canUseCustomConfig = canUseCustomConfig,
              showNotificationSettings = showNotificationSettings,
              hasNotificationPermission = hasNotificationPermission,
              onSsidChanged = onSsidChanged,
              onPasswordChanged = onPasswordChanged,
              onPortChanged = onPortChanged,
              onOpenBatterySettings = onOpenBatterySettings,
              onToggleKeepWakeLock = onToggleKeepWakeLock,
              onSelectBand = onSelectBand,
              onRequestNotificationPermission = onRequestNotificationPermission,
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
    showNotificationSettings: Boolean,
    hasNotificationPermission: Boolean,
    onSsidChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onPortChanged: (String) -> Unit,
    onSelectBand: (ServerNetworkBand) -> Unit,
    onOpenBatterySettings: () -> Unit,
    onToggleKeepWakeLock: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
) {
  renderNetworkInformation(
      itemModifier = Modifier.fillMaxWidth(),
      isEditable = isEditable,
      canUseCustomConfig = canUseCustomConfig,
      appName = appName,
      state = state,
      onSsidChanged = onSsidChanged,
      onPasswordChanged = onPasswordChanged,
      onPortChanged = onPortChanged,
      onSelectBand = onSelectBand,
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
        hasPermission = hasNotificationPermission,
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
    canUseCustomConfig: Boolean,
    onSsidChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onPortChanged: (String) -> Unit,
    onSelectBand: (ServerNetworkBand) -> Unit,
) {

  item {
    val showErrorHintMessage = remember(state) { state.wiDiStatus is RunningStatus.Error }

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
    AnimatedVisibility(
        visible = state.requiresPermissions,
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
      val ssid =
          remember(
              canUseCustomConfig,
              state.ssid,
          ) {
            if (canUseCustomConfig) state.ssid else SYSTEM_DEFINED
          }

      val handleSsidChanged by rememberUpdatedState(onSsidChanged)

      StatusEditor(
          modifier =
              itemModifier
                  .padding(bottom = MaterialTheme.keylines.baseline)
                  .padding(horizontal = MaterialTheme.keylines.content),
          enabled = canUseCustomConfig,
          title = "HOTSPOT NAME/SSID",
          value = ssid,
          onChange = handleSsidChanged,
      )
    }

    item {
      val password =
          remember(
              canUseCustomConfig,
              state.password,
          ) {
            if (canUseCustomConfig) state.password else SYSTEM_DEFINED
          }

      val handlePasswordChanged by rememberUpdatedState(onPasswordChanged)

      StatusEditor(
          modifier =
              itemModifier
                  .padding(bottom = MaterialTheme.keylines.baseline)
                  .padding(horizontal = MaterialTheme.keylines.content),
          enabled = canUseCustomConfig,
          title = "HOTSPOT PASSWORD",
          value = password,
          onChange = handlePasswordChanged,
          keyboardOptions =
              KeyboardOptions(
                  keyboardType = KeyboardType.Password,
              ),
      )
    }

    item {
      val port = remember(state.port) { "${state.port}" }

      val handlePortChanged by rememberUpdatedState(onPortChanged)

      StatusEditor(
          modifier =
              itemModifier
                  .padding(bottom = MaterialTheme.keylines.baseline)
                  .padding(horizontal = MaterialTheme.keylines.content),
          title = "PROXY PORT",
          value = port,
          onChange = handlePortChanged,
          keyboardOptions =
              KeyboardOptions(
                  keyboardType = KeyboardType.Number,
              ),
      )
    }
  } else {
    item {
      val ssid = remember(state.group) { state.group?.ssid ?: "NO SSID" }

      StatusItem(
          modifier =
              itemModifier
                  .padding(bottom = MaterialTheme.keylines.baseline)
                  .padding(horizontal = MaterialTheme.keylines.content),
          title = "HOTSPOT NAME/SSID",
          value = ssid,
          valueStyle =
              MaterialTheme.typography.h6.copy(
                  fontWeight = FontWeight.W400,
              ),
      )
    }

    item {
      val password = remember(state.group) { state.group?.password ?: "NO PASSWORD" }

      StatusItem(
          modifier =
              itemModifier
                  .padding(bottom = MaterialTheme.keylines.content * 2)
                  .padding(horizontal = MaterialTheme.keylines.content),
          title = "HOTSPOT PASSWORD",
          value = password,
          valueStyle =
              MaterialTheme.typography.h6.copy(
                  fontWeight = FontWeight.W400,
              ),
      )
    }

    item {
      val ip = remember(state.ip) { state.ip.ifBlank { "NO IP ADDRESS" } }
      StatusItem(
          modifier =
              itemModifier
                  .padding(bottom = MaterialTheme.keylines.baseline)
                  .padding(horizontal = MaterialTheme.keylines.content),
          title = "PROXY URL/HOSTNAME",
          value = ip,
          valueStyle =
              MaterialTheme.typography.h6.copy(
                  fontWeight = FontWeight.W400,
              ),
      )
    }

    item {
      val port = remember(state.port) { if (state.port <= 1024) "INVALID PORT" else "${state.port}" }

      StatusItem(
          modifier =
              itemModifier
                  .padding(bottom = MaterialTheme.keylines.baseline)
                  .padding(horizontal = MaterialTheme.keylines.content),
          title = "PROXY PORT",
          value = port,
          valueStyle =
              MaterialTheme.typography.h6.copy(
                  fontWeight = FontWeight.W400,
              ),
      )
    }
  }
  item {
    val handleSelectBand by rememberUpdatedState(onSelectBand)

    NetworkBands(
        modifier =
            itemModifier
                .padding(top = MaterialTheme.keylines.content)
                .padding(horizontal = MaterialTheme.keylines.content),
        isEnabled = canUseCustomConfig,
        isEditable = isEditable,
        band = state.band,
        onSelectBand = handleSelectBand,
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
    val handleToggleKeepWakeLock by rememberUpdatedState(onToggleKeepWakeLock)
    CpuWakelock(
        modifier =
            itemModifier
                .padding(horizontal = MaterialTheme.keylines.content)
                .padding(bottom = MaterialTheme.keylines.content),
        isEditable = isEditable,
        appName = appName,
        keepWakeLock = state.keepWakeLock,
        onToggleKeepWakeLock = handleToggleKeepWakeLock,
    )
  }

  item {
    val handleOpenBatterySettings by rememberUpdatedState(onDisableBatteryOptimizations)

    BatteryOptimization(
        modifier = itemModifier.padding(horizontal = MaterialTheme.keylines.content),
        isEditable = isEditable,
        appName = appName,
        isBatteryOptimizationDisabled = state.isBatteryOptimizationsIgnored,
        onDisableBatteryOptimizations = handleOpenBatterySettings,
    )
  }
}

@Preview
@Composable
private fun PreviewStatusScreen() {
  StatusScreen(
      state = MutableStatusViewState(),
      appName = "TEST",
      hasNotificationPermission = false,
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
      onToggle = {},
  )
}
