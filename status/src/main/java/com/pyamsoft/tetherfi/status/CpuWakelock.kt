package com.pyamsoft.tetherfi.status

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pyamsoft.pydroid.ui.widget.MaterialCheckable

@Composable
internal fun CpuWakelock(
    modifier: Modifier = Modifier,
    appName: String,
    isEditable: Boolean,
    keepWakeLock: Boolean,
    onToggleKeepWakeLock: () -> Unit,
) {
  MaterialCheckable(
      modifier = modifier,
      isEditable = isEditable,
      condition = keepWakeLock,
      title = "Keep CPU Awake",
      description =
          """This will significantly improve $appName when the screen is off. Without it, you may notice extreme network slow down.
            |
            |This will use more battery, as it prevents your device from entering a deep-sleep state.
            |(recommended)"""
              .trimMargin(),
      onClick = onToggleKeepWakeLock,
  )
}
