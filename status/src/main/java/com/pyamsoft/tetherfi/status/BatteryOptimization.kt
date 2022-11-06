package com.pyamsoft.tetherfi.status

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.pydroid.ui.defaults.CardDefaults
import com.pyamsoft.tetherfi.ui.icons.RadioButtonUnchecked

@Composable
internal fun BatteryOptimization(
    modifier: Modifier = Modifier,
    appName: String,
    isBatteryOptimizationDisabled: Boolean,
    onDisableBatteryOptimizations: () -> Unit,
) {
  val color =
      if (isBatteryOptimizationDisabled) MaterialTheme.colors.primary
      else MaterialTheme.colors.onSurface
  val highAlpha = ContentAlpha.high
  // High alpha when selected
  val mediumAlpha = if (isBatteryOptimizationDisabled) ContentAlpha.high else ContentAlpha.medium

  Box(
      modifier =
          modifier.border(
              width = 2.dp,
              color = color.copy(alpha = mediumAlpha),
              shape = MaterialTheme.shapes.medium,
          ),
  ) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.Elevation,
    ) {
      Column(
          modifier =
              Modifier.fillMaxWidth()
                  .clickable { onDisableBatteryOptimizations() }
                  .padding(MaterialTheme.keylines.content),
      ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
          Icon(
              imageVector =
                  if (isBatteryOptimizationDisabled) Icons.Filled.CheckCircle
                  else Icons.Filled.RadioButtonUnchecked,
              contentDescription = "Disable Battery Optimizations",
              tint = color.copy(alpha = highAlpha),
          )

          Column(
              modifier = Modifier.padding(start = MaterialTheme.keylines.content),
          ) {
            Text(
                text = "Disable Battery Optimizations",
                style =
                    MaterialTheme.typography.body1.copy(
                        color = color.copy(alpha = highAlpha),
                        fontWeight = FontWeight.W700,
                    ),
            )

            Text(
                text =
                    """This will allow $appName to run at maximum performance.
                        |
                        |This will significantly enhance your networking experience but may use more battery.
                        |(recommended)"""
                        .trimMargin(),
                style =
                    MaterialTheme.typography.caption.copy(
                        color = MaterialTheme.colors.onSurface.copy(alpha = mediumAlpha),
                        fontWeight = FontWeight.W400,
                    ),
            )
          }
        }
      }
    }
  }
}
