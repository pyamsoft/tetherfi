package com.pyamsoft.tetherfi.status

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Card
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.pydroid.ui.defaults.CardDefaults
import com.pyamsoft.tetherfi.ui.icons.RadioButtonUnchecked

@Composable
internal fun CpuWakelock(
    modifier: Modifier = Modifier,
    isEditable: Boolean,
    keepWakeLock: Boolean,
    onToggleKeepWakeLock: () -> Unit,
) {
  val color = if (keepWakeLock) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface
  val highAlpha = if (isEditable) ContentAlpha.high else ContentAlpha.disabled
  val mediumAlpha =
      if (isEditable) {
        // High alpha when selected
        if (keepWakeLock) ContentAlpha.high else ContentAlpha.medium
      } else ContentAlpha.disabled

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
      Row(
          modifier =
              Modifier.fillMaxWidth()
                  .clickable(enabled = isEditable) { onToggleKeepWakeLock() }
                  .padding(MaterialTheme.keylines.content),
          verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
            modifier = Modifier.size(16.dp),
            imageVector =
                if (keepWakeLock) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
            contentDescription = if (keepWakeLock) "CPU kept awake" else "CPU not kept awake",
            tint = color.copy(alpha = highAlpha),
        )

        Text(
            modifier = Modifier.padding(start = MaterialTheme.keylines.content),
            text = "Keep CPU awake for full performance",
            style =
                MaterialTheme.typography.body2.copy(
                    color = color.copy(alpha = highAlpha),
                    fontWeight = FontWeight.W700,
                ),
        )
      }
    }
  }
}
