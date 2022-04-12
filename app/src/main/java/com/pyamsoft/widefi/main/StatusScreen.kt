package com.pyamsoft.widefi.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.pyamsoft.widefi.server.status.RunningStatus

@Composable
internal fun StatusScreen(
    modifier: Modifier = Modifier,
    state: MainViewState,
) {
  val group = state.group
  val proxyStatus = state.proxyStatus
  val wiDiStatus = state.wiDiStatus

  Column(
      modifier = modifier,
  ) {
    Text(
        text = "SSID=${group?.ssid ?: "NULL"}",
        style = MaterialTheme.typography.body1,
    )

    Text(
        text = "PASSWORD=${group?.password ?: "NULL"}",
        style = MaterialTheme.typography.body1,
    )

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

  Row(
      modifier = modifier,
      verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
        text = title,
        style = MaterialTheme.typography.body2,
    )
    Text(
        text = text,
        style = MaterialTheme.typography.body2,
        color = color,
    )
  }
}
