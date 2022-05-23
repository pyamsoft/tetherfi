package com.pyamsoft.tetherfi.status

import androidx.annotation.CheckResult
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.tetherfi.server.ServerDefaults
import com.pyamsoft.tetherfi.server.status.RunningStatus

@Composable
fun StatusScreen(
    modifier: Modifier = Modifier,
    state: StatusViewState,
    onToggle: () -> Unit,
    onSsidChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onPortChanged: (String) -> Unit,
    onOpenBatterySettings: () -> Unit,
) {
  val proxyStatus = state.proxyStatus
  val wiDiStatus = state.wiDiStatus
  val isLoaded = state.preferencesLoaded

  val isButtonEnabled =
      remember(wiDiStatus) {
        wiDiStatus is RunningStatus.Running ||
            wiDiStatus is RunningStatus.NotRunning ||
            wiDiStatus is RunningStatus.Error
      }

  val buttonText =
      remember(wiDiStatus) {
        when (wiDiStatus) {
          is RunningStatus.Error -> "TetherFi Error"
          is RunningStatus.NotRunning -> "Turn TetherFi ON"
          is RunningStatus.Running -> "Turn TetherFi OFF"
          else -> "TetherFi is thinking..."
        }
      }

  val scaffoldState = rememberScaffoldState()

  val loadedContent =
      prepareLoadedContent(
          state = state,
          onSsidChanged = onSsidChanged,
          onPasswordChanged = onPasswordChanged,
          onPortChanged = onPortChanged,
          onOpenBatterySettings = onOpenBatterySettings,
      )

  Scaffold(
      modifier = modifier,
      scaffoldState = scaffoldState,
  ) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
    ) {
      item {
        Column(
            modifier =
                Modifier.padding(top = MaterialTheme.keylines.content)
                    .padding(horizontal = MaterialTheme.keylines.content),
        ) {
          Button(
              enabled = isButtonEnabled,
              onClick = onToggle,
          ) {
            Text(
                text = buttonText,
            )
          }
        }
      }

      item {
        Column(
            Modifier.padding(top = MaterialTheme.keylines.content)
                .padding(horizontal = MaterialTheme.keylines.content),
        ) {
          DisplayStatus(
              modifier = Modifier.padding(bottom = MaterialTheme.keylines.baseline),
              title = "WiFi Network Status:",
              status = wiDiStatus,
          )

          DisplayStatus(
              title = "Proxy Status:",
              status = proxyStatus,
          )
        }
      }

      if (isLoaded) {
        loadedContent()
      } else {
        item {
          Column(
              modifier =
                  Modifier.padding(top = MaterialTheme.keylines.content)
                      .padding(horizontal = MaterialTheme.keylines.content),
          ) {
            Box(
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
          }
        }
      }
    }
  }
}

@Composable
@CheckResult
private fun prepareLoadedContent(
    state: StatusViewState,
    onSsidChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onPortChanged: (String) -> Unit,
    onOpenBatterySettings: () -> Unit,
): LazyListScope.() -> Unit {
  val canUseCustomConfig = remember { ServerDefaults.canUseCustomConfig() }
  val isEditable =
      remember(state.wiDiStatus) {
        when (state.wiDiStatus) {
          is RunningStatus.Running, is RunningStatus.Starting, is RunningStatus.Stopping -> false
          else -> true
        }
      }

  val group = state.group
  val ssid =
      remember(
          isEditable,
          group,
          state.ssid,
          canUseCustomConfig,
      ) {
        if (isEditable) {
          if (canUseCustomConfig) {
            state.ssid
          } else {
            "SYSTEM DEFINED SSID"
          }
        } else {
          group?.ssid ?: "NO SSID"
        }
      }
  val password =
      remember(
          isEditable,
          group,
          state.password,
          canUseCustomConfig,
      ) {
        if (isEditable) {
          if (canUseCustomConfig) {
            state.password
          } else {
            "SYSTEM DEFINED PASSWORD"
          }
        } else {
          group?.password ?: "NO PASSWORD"
        }
      }

  val ip = remember(state.ip) { state.ip.ifBlank { "NO IP ADDRESS" } }
  val port = remember(state.port) { if (state.port <= 0) "NO PORT" else "${state.port}" }
  val bandName = remember(state.band) { state.band?.name ?: "AUTO" }
  val showInstructions = remember(isEditable) { !isEditable }

  return remember(
      ssid,
      password,
      port,
      ip,
      bandName,
      onSsidChanged,
      onPasswordChanged,
      onPortChanged,
  ) {
    {
      item {
        NetworkInformation(
            modifier = Modifier.padding(MaterialTheme.keylines.content),
            isEditable = isEditable,
            canUseCustomConfig = canUseCustomConfig,
            ssid = ssid,
            password = password,
            port = port,
            ip = ip,
            bandName = bandName,
            onSsidChanged = onSsidChanged,
            onPasswordChanged = onPasswordChanged,
            onPortChanged = onPortChanged,
        )
      }

      item {
        BatteryInstructions(
            modifier = Modifier.padding(MaterialTheme.keylines.content),
            show = showInstructions,
            state = state,
            onOpenBatterySettings = onOpenBatterySettings,
        )
      }

      item {
        ConnectionInstructions(
            modifier = Modifier.padding(MaterialTheme.keylines.content),
            show = showInstructions,
            ssid = ssid,
            password = password,
            port = port,
            ip = ip,
        )
      }
    }
  }
}

@Composable
private fun BatteryInstructions(
    modifier: Modifier = Modifier,
    show: Boolean,
    state: StatusViewState,
    onOpenBatterySettings: () -> Unit,
) {
  val isIgnored = state.isBatteryOptimizationsIgnored

  AnimatedVisibility(
      visible = show,
      modifier = modifier,
  ) {
    Column {
      Text(
          text = "How to Improve Performance",
          style = MaterialTheme.typography.h6,
      )

      Text(
          modifier = Modifier.padding(top = MaterialTheme.keylines.baseline),
          text =
              "You can disable Android Battery Optimizations to ensure that the TetherFi proxy server is running at full performance.",
          style = MaterialTheme.typography.body1,
      )

      if (isIgnored) {
        Row(
            modifier = Modifier.padding(top = MaterialTheme.keylines.baseline),
            verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
              modifier = Modifier.padding(end = MaterialTheme.keylines.baseline),
              imageVector = Icons.Filled.CheckCircle,
              contentDescription = "Battery Optimizations Ignored",
              tint = Color.Green,
          )
          Text(
              text = "Battery Optimizations Ignored.",
              style = MaterialTheme.typography.body1,
          )
        }
      } else {
        Button(
            modifier = Modifier.padding(top = MaterialTheme.keylines.baseline),
            onClick = onOpenBatterySettings,
        ) {
          Text(
              text = "Open Battery Settings",
          )
        }
      }
    }
  }
}

@Composable
private fun ConnectionInstructions(
    modifier: Modifier = Modifier,
    show: Boolean,
    ssid: String,
    password: String,
    port: String,
    ip: String,
) {
  AnimatedVisibility(
      visible = show,
      modifier = modifier,
  ) {
    Column {
      Text(
          text = "How to Connect",
          style = MaterialTheme.typography.h6,
      )

      Text(
          modifier = Modifier.padding(top = MaterialTheme.keylines.baseline),
          text =
              "First, make sure this device (Device 1) has an active connection to the Internet. You will be sharing this device's connection, so if this device cannot access the Internet, nothing can.",
          style = MaterialTheme.typography.body1,
      )

      Text(
          modifier = Modifier.padding(top = MaterialTheme.keylines.baseline),
          text =
              "Then, on the device you want to connect (Device 2) to the Internet, go to the Wi-Fi settings. In the Wi-Fi network settings, connect to the network labeled: \"${ssid}\"",
          style = MaterialTheme.typography.body1,
      )

      Text(
          modifier = Modifier.padding(top = MaterialTheme.keylines.baseline),
          text = "Connect to the \"${ssid}\" network using the password: \"${password}\"",
          style = MaterialTheme.typography.body1,
      )

      Text(
          modifier = Modifier.padding(top = MaterialTheme.keylines.baseline),
          text =
              "Once you are connected to the network, you will need to go to the Proxy settings (Device 2), and set the following proxy information as an HTTP and HTTPS proxy.",
          style = MaterialTheme.typography.body1,
      )

      Text(
          modifier = Modifier.padding(top = MaterialTheme.keylines.baseline),
          text = "Proxy URL/Hostname: $ip",
          style = MaterialTheme.typography.body1,
      )

      Text(
          text = "Proxy Port: $port",
          style = MaterialTheme.typography.body1,
      )

      Text(
          text = "Leave blank any Proxy username or password or authentication information.",
          style = MaterialTheme.typography.body1,
      )

      Text(
          modifier = Modifier.padding(top = MaterialTheme.keylines.baseline),
          text =
              "Once the network is connected and the proxy information has been set, you should be able to access the Internet on Device 2! You may need to setup Proxy settings for individual applications on Device 2, as every application is different.",
          style = MaterialTheme.typography.body1,
      )
    }
  }
}

@Composable
private fun NetworkInformation(
    modifier: Modifier = Modifier,
    isEditable: Boolean,
    canUseCustomConfig: Boolean,
    ssid: String,
    password: String,
    port: String,
    ip: String,
    bandName: String,
    onSsidChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onPortChanged: (String) -> Unit,
) {

  Crossfade(
      modifier = modifier,
      targetState = isEditable,
  ) { editable ->
    Column {
      if (editable) {
        Editor(
            modifier = Modifier.padding(bottom = MaterialTheme.keylines.baseline),
            enabled = canUseCustomConfig,
            title = "SSID",
            value = ssid,
            onChange = onSsidChanged,
        )

        Editor(
            modifier = Modifier.padding(bottom = MaterialTheme.keylines.baseline),
            enabled = canUseCustomConfig,
            title = "PASSWORD",
            value = password,
            onChange = onPasswordChanged,
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                ),
        )

        Editor(
            modifier = Modifier.padding(bottom = MaterialTheme.keylines.baseline),
            title = "PORT",
            value = port,
            onChange = onPortChanged,
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                ),
        )
      } else {
        Item(
            modifier = Modifier.padding(bottom = MaterialTheme.keylines.baseline),
            title = "SSID",
            value = ssid,
        )

        Item(
            modifier = Modifier.padding(bottom = MaterialTheme.keylines.baseline),
            title = "PASSWORD",
            value = password,
        )

        Item(
            modifier = Modifier.padding(top = MaterialTheme.keylines.content),
            title = "IP",
            value = ip,
        )

        Item(
            modifier = Modifier.padding(bottom = MaterialTheme.keylines.baseline),
            title = "PORT",
            value = port,
        )

        Item(
            modifier = Modifier.padding(bottom = MaterialTheme.keylines.baseline),
            title = "BAND",
            value = bandName,
        )
      }
    }
  }
}

@Composable
private fun Editor(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    title: String,
    value: String,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onChange: (String) -> Unit,
) {
  Column(
      modifier = modifier,
  ) {
    TextField(
        enabled = enabled,
        keyboardOptions = keyboardOptions,
        value = value,
        onValueChange = onChange,
        label = {
          Text(
              text = title,
          )
        },
    )
  }
}

@Composable
private fun DisplayStatus(
    modifier: Modifier = Modifier,
    title: String,
    status: RunningStatus,
) {
  val text =
      remember(status) {
        when (status) {
          is RunningStatus.Error -> "Error: ${status.message}"
          is RunningStatus.NotRunning -> "Not Running"
          is RunningStatus.Running -> "Running"
          is RunningStatus.Starting -> "Starting"
          is RunningStatus.Stopping -> "Stopping"
        }
      }

  val color =
      remember(status) {
        when (status) {
          is RunningStatus.Error -> Color.Red
          is RunningStatus.NotRunning -> Color.Unspecified
          is RunningStatus.Running -> Color.Green
          is RunningStatus.Starting -> Color.Cyan
          is RunningStatus.Stopping -> Color.Magenta
        }
      }

  Item(
      modifier = modifier,
      title = title,
      value = text,
      color = color,
  )
}

@Composable
private fun Item(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    color: Color = Color.Unspecified,
) {
  Column(
      modifier = modifier,
  ) {
    Text(
        text = title,
        style =
            MaterialTheme.typography.caption.copy(
                fontWeight = FontWeight.Bold,
            ),
    )
    Text(
        text = value,
        style = MaterialTheme.typography.body1,
        color = color,
    )
  }
}
