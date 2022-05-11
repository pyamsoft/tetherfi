package com.pyamsoft.widefi.status

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.widefi.server.status.RunningStatus

@Composable
fun StatusScreen(
    modifier: Modifier = Modifier,
    state: StatusViewState,
    onToggle: () -> Unit,
    onSsidChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onPortChanged: (String) -> Unit,
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
          is RunningStatus.Error -> "WideFi Error"
          is RunningStatus.NotRunning -> "Turn WideFi ON"
          is RunningStatus.Running -> "Turn WideFi OFF"
          else -> "WideFi is thinking..."
        }
      }

  val isEditable = remember(state.wiDiStatus) { state.wiDiStatus == RunningStatus.NotRunning }

  val scaffoldState = rememberScaffoldState()

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
            modifier =
                Modifier.padding(top = MaterialTheme.keylines.content)
                    .padding(horizontal = MaterialTheme.keylines.content),
        ) {
          DisplayStatus(
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

        item {
          NetworkInformation(
              modifier =
                  Modifier.padding(top = MaterialTheme.keylines.content)
                      .padding(horizontal = MaterialTheme.keylines.content),
              state = state,
              isEditable = isEditable,
              onSsidChanged = onSsidChanged,
              onPasswordChanged = onPasswordChanged,
              onPortChanged = onPortChanged,
          )
        }

        item {
          ConnectionInstructions(
              modifier =
                  Modifier.padding(top = MaterialTheme.keylines.content)
                      .padding(horizontal = MaterialTheme.keylines.content),
              state = state,
              isEditable = isEditable,
          )
        }
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
private fun ConnectionInstructions(
    modifier: Modifier = Modifier,
    state: StatusViewState,
    isEditable: Boolean,
) {
  val group = state.group
  val ssid =
      remember(isEditable, group, state.ssid) {
        if (isEditable) state.ssid else group?.ssid ?: "--"
      }
  val password =
      remember(isEditable, group, state.password) {
        if (isEditable) state.password else group?.password ?: "--"
      }

  val ip = remember(state.ip) { state.ip.ifBlank { "--" } }
  val port = remember(state.port) { if (state.port <= 0) "--" else "${state.port}" }

  AnimatedVisibility(
      visible = !isEditable,
      modifier = modifier,
  ) {
    Column(
        modifier = modifier,
    ) {
      Text(
          text = "How to connect",
          style = MaterialTheme.typography.h6,
      )

      Text(
          modifier = Modifier.padding(top = MaterialTheme.keylines.baseline),
          text =
              "First, make sure this device (Device 1) has an active connection to the Internet. You will be sharing this device's connection, so if this device cannot access the Internet, nothing can.",
          style = MaterialTheme.typography.body2,
      )

      Text(
          modifier = Modifier.padding(top = MaterialTheme.keylines.baseline),
          text =
              "Then, on the device you want to connect (Device 2) to the Internet, go to the Wi-Fi settings. In the Wi-Fi network settings, connect to the network labeled: \"${ssid}\"",
          style = MaterialTheme.typography.body2,
      )

      Text(
          modifier = Modifier.padding(top = MaterialTheme.keylines.baseline),
          text = "Connect to the \"${ssid}\" network using the password: \"${password}\"",
          style = MaterialTheme.typography.body2,
      )

      Text(
          modifier = Modifier.padding(top = MaterialTheme.keylines.baseline),
          text =
              "Once you are connected to the network, you will need to go to the Proxy settings (Device 2), and set the following proxy information as an HTTP and HTTPS proxy.",
          style = MaterialTheme.typography.body2,
      )

      Text(
          modifier = Modifier.padding(top = MaterialTheme.keylines.baseline),
          text = "Proxy URL/Hostname: $ip",
          style = MaterialTheme.typography.body2,
      )

      Text(
          text = "Proxy Port: $port",
          style = MaterialTheme.typography.body2,
      )

      Text(
          text = "Leave blank any Proxy username or password or authentication information.",
          style = MaterialTheme.typography.body2,
      )

      Text(
          modifier = Modifier.padding(top = MaterialTheme.keylines.baseline),
          text =
              "Once the network is connected and the proxy information has been set, you should be able to access the Internet on Device 2! You may need to setup Proxy settings for individual applications on Device 2, as every application is different.",
          style = MaterialTheme.typography.body2,
      )
    }
  }
}

@Composable
private fun NetworkInformation(
    modifier: Modifier = Modifier,
    state: StatusViewState,
    isEditable: Boolean,
    onSsidChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onPortChanged: (String) -> Unit,
) {
  val group = state.group
  val ssid =
      remember(isEditable, group, state.ssid) {
        if (isEditable) state.ssid else group?.ssid ?: "--"
      }
  val password =
      remember(isEditable, group, state.password) {
        if (isEditable) state.password else group?.password ?: "--"
      }

  val ip = remember(state.ip) { state.ip.ifBlank { "--" } }
  val port = remember(state.port) { if (state.port <= 0) "--" else "${state.port}" }
  val bandName = remember(state.band) { state.band?.name ?: "--" }

  Crossfade(
      modifier = modifier,
      targetState = isEditable,
  ) { editable ->
    Column {
      if (editable) {
        Editor(
            title = "SSID",
            value = ssid,
            onChange = onSsidChanged,
        )

        Editor(
            title = "PASSWORD",
            value = password,
            onChange = onPasswordChanged,
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                ),
        )

        Editor(
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
            title = "SSID",
            value = ssid,
        )

        Item(
            title = "PASSWORD",
            value = password,
        )

        Item(
            modifier = Modifier.padding(top = MaterialTheme.keylines.content),
            title = "IP",
            value = ip,
        )

        Item(
            title = "PORT",
            value = port,
        )

        Item(
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
    title: String,
    value: String,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onChange: (String) -> Unit,
) {
  Column(
      modifier = modifier,
  ) {
    TextField(
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
        style = MaterialTheme.typography.body2,
        color = color,
    )
  }
}
