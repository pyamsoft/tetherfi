package com.pyamsoft.tetherfi.status

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pyamsoft.tetherfi.ui.MaterialCheckable

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
          """This will significantly improve $appName performance.
            |
            |This will sometimes use more battery, as it may prevent your device from entering a deep sleep state.
            |(recommended)"""
              .trimMargin(),
      onClick = onToggleKeepWakeLock,
  )
}
