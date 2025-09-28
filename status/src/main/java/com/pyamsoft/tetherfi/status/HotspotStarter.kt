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

import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.pyamsoft.pydroid.ui.haptics.LocalHapticManager
import com.pyamsoft.tetherfi.server.status.RunningStatus
import org.jetbrains.annotations.TestOnly

@Composable
internal fun HotspotStarter(
    modifier: Modifier = Modifier,
    isButtonEnabled: Boolean,
    hotspotStatus: RunningStatus,
    appName: String,
    onToggleProxy: () -> Unit,
) {
  val context = LocalContext.current
  val hapticManager = LocalHapticManager.current

  val buttonTextResId =
      remember(
          hotspotStatus,
          appName,
      ) {
        when (hotspotStatus) {
          is RunningStatus.Error -> R.string.hotspot_error
          is RunningStatus.NotRunning -> R.string.start_hotspot
          is RunningStatus.Running -> R.string.stop_hotspot
          else -> R.string.is_thinking
        }
      }

  val buttonText =
      remember(
          context,
          appName,
          buttonTextResId,
      ) {
        context.getString(buttonTextResId, appName)
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
            MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.W700,
            ),
    )
  }
}

@TestOnly
@Composable
private fun PreviewHotspotStarter(
    status: RunningStatus,
    isButtonEnabled: Boolean,
) {
  HotspotStarter(
      isButtonEnabled = isButtonEnabled,
      hotspotStatus = status,
      appName = "TEST",
      onToggleProxy = {},
  )
}

@Composable
@Preview(showBackground = true)
private fun PreviewHotspotStarterNotRunningEnabled() {
  PreviewHotspotStarter(isButtonEnabled = true, status = RunningStatus.NotRunning)
}

@Composable
@Preview(showBackground = true)
private fun PreviewHotspotStarterRunningEnabled() {
  PreviewHotspotStarter(isButtonEnabled = true, status = RunningStatus.Running)
}

@Composable
@Preview(showBackground = true)
private fun PreviewHotspotStarterStartingEnabled() {
  PreviewHotspotStarter(isButtonEnabled = true, status = RunningStatus.Starting)
}

@Composable
@Preview(showBackground = true)
private fun PreviewHotspotStarterStoppingEnabled() {
  PreviewHotspotStarter(isButtonEnabled = true, status = RunningStatus.Stopping)
}

@Composable
@Preview(showBackground = true)
private fun PreviewHotspotStarterHotspotErrorEnabled() {
  PreviewHotspotStarter(
      isButtonEnabled = true,
      status = RunningStatus.HotspotError(RuntimeException("TEST")),
  )
}

@Composable
@Preview(showBackground = true)
private fun PreviewHotspotStarterProxyErrorEnabled() {
  PreviewHotspotStarter(
      isButtonEnabled = true,
      status = RunningStatus.ProxyError(RuntimeException("TEST")),
  )
}

@Composable
@Preview(showBackground = true)
private fun PreviewHotspotStarterNotRunningDisabled() {
  PreviewHotspotStarter(isButtonEnabled = false, status = RunningStatus.NotRunning)
}

@Composable
@Preview(showBackground = true)
private fun PreviewHotspotStarterRunningDisabled() {
  PreviewHotspotStarter(isButtonEnabled = false, status = RunningStatus.Running)
}

@Composable
@Preview(showBackground = true)
private fun PreviewHotspotStarterStartingDisabled() {
  PreviewHotspotStarter(isButtonEnabled = false, status = RunningStatus.Starting)
}

@Composable
@Preview(showBackground = true)
private fun PreviewHotspotStarterStoppingDisabled() {
  PreviewHotspotStarter(isButtonEnabled = false, status = RunningStatus.Stopping)
}

@Composable
@Preview(showBackground = true)
private fun PreviewHotspotStarterHotspotErrorDisabled() {
  PreviewHotspotStarter(
      isButtonEnabled = false,
      status = RunningStatus.HotspotError(RuntimeException("TEST")),
  )
}

@Composable
@Preview(showBackground = true)
private fun PreviewHotspotStarterProxyErrorDisabled() {
  PreviewHotspotStarter(
      isButtonEnabled = false,
      status = RunningStatus.ProxyError(RuntimeException("TEST")),
  )
}
