package com.pyamsoft.tetherfi.status

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pyamsoft.pydroid.ui.widget.MaterialCheckable

@Composable
internal fun BatteryOptimization(
    modifier: Modifier = Modifier,
    isEditable: Boolean,
    appName: String,
    isBatteryOptimizationDisabled: Boolean,
    onDisableBatteryOptimizations: () -> Unit,
) {
  MaterialCheckable(
      modifier = modifier,
      isEditable = isEditable,
      condition = isBatteryOptimizationDisabled,
      title = "Ignore Battery Optimizations",
      description =
          """This will allow $appName to run at maximum performance.
            |
            |This will significantly enhance your networking experience but may use more battery.
            |(recommended)"""
              .trimMargin(),
      onClick = onDisableBatteryOptimizations,
  )
}
