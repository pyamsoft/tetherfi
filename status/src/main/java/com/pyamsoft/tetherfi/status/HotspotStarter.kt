package com.pyamsoft.tetherfi.status

import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.pyamsoft.pydroid.ui.haptics.LocalHapticManager
import com.pyamsoft.tetherfi.server.status.RunningStatus

@Composable
internal fun HotspotStarter(
    modifier: Modifier = Modifier,
    isButtonEnabled: Boolean,
    hotspotStatus: RunningStatus,
    appName: String,
    onToggleProxy: () -> Unit,
) {
  val hapticManager = LocalHapticManager.current

  val buttonText =
      remember(
          hotspotStatus,
          appName,
      ) {
        when (hotspotStatus) {
          is RunningStatus.Error -> "$appName Hotspot Error"
          is RunningStatus.NotRunning -> "Start $appName Hotspot"
          is RunningStatus.Running -> "Stop $appName Hotspot"
          else -> "$appName is thinking..."
        }
      }

  Button(
      modifier = modifier,
      enabled = isButtonEnabled,
      onClick = {
        hapticManager?.actionButtonPress()
        onToggleProxy()
      },
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
