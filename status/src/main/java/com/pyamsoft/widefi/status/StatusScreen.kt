package com.pyamsoft.widefi.status

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.widefi.server.status.RunningStatus

@Composable
fun StatusScreen(
    modifier: Modifier = Modifier,
    state: StatusViewState,
    onToggle: () -> Unit,
) {
  val proxyStatus = state.proxyStatus
  val wiDiStatus = state.wiDiStatus
  val isLoaded = state.preferencesLoaded

  val group = state.group
  val ip = remember(state.ip) { state.ip.ifBlank { "UNDEFINED" } }
  val port = remember(state.port) { if (state.port <= 0) "UNSET" else "${state.port}" }

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

  LazyColumn(
      modifier = modifier,
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
        Column(
            modifier =
                Modifier.padding(top = MaterialTheme.keylines.content)
                    .padding(horizontal = MaterialTheme.keylines.content),
        ) {
          Item(
              title = "SSID",
              value = group?.ssid ?: "NULL",
          )

          Item(
              title = "PASSWORD",
              value = group?.password ?: "NULL",
          )

          Item(
              title = "BAND",
              value = group?.band?.name ?: "NULL",
          )
        }
      }

      item {
        Column(
            modifier =
                Modifier.padding(top = MaterialTheme.keylines.content)
                    .padding(horizontal = MaterialTheme.keylines.content),
        ) {
          Item(
              title = "IP",
              value = ip,
          )

          Item(
              title = "PORT",
              value = port,
          )
        }
      }
    } else {
      item {
        Column(
            modifier =
                Modifier.padding(top = MaterialTheme.keylines.content)
                    .padding(horizontal = MaterialTheme.keylines.content),
        ) { CircularProgressIndicator() }
      }
    }
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
  Row(
      modifier = modifier,
      verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
        text = title,
        style = MaterialTheme.typography.body2,
    )
    Text(
        modifier = Modifier.padding(start = MaterialTheme.keylines.baseline),
        text = value,
        style = MaterialTheme.typography.body2,
        color = color,
    )
  }
}
