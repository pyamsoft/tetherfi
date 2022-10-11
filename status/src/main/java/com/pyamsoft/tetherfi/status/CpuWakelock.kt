package com.pyamsoft.tetherfi.status

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.tetherfi.ui.icons.RadioButtonUnchecked

@Composable
internal fun CpuWakelock(
    modifier: Modifier = Modifier,
    isEditable: Boolean,
    keepWakeLock: Boolean,
    onToggleKeepWakeLock: () -> Unit,
) {
  Box(
      modifier =
          modifier.border(
              width = 2.dp,
              color =
                  (if (keepWakeLock) MaterialTheme.colors.primary
                      else MaterialTheme.colors.onSurface)
                      .copy(
                          alpha = 0.6F,
                      ),
              shape = MaterialTheme.shapes.medium,
          ),
  ) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
      Row(
          modifier =
              Modifier.fillMaxWidth()
                  .clickable {
                    if (isEditable) {
                      onToggleKeepWakeLock()
                    }
                  }
                  .padding(MaterialTheme.keylines.content),
          verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
            modifier = Modifier.size(16.dp),
            imageVector =
                if (keepWakeLock) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
            contentDescription = if (keepWakeLock) "CPU kept awake" else "CPU not kept awake",
            tint =
                if (keepWakeLock) MaterialTheme.colors.primary
                else
                    MaterialTheme.colors.onSurface.copy(
                        alpha = 0.6F,
                    ),
        )

        Text(
            modifier = Modifier.padding(start = MaterialTheme.keylines.content),
            text = "Keep CPU awake for full performance",
            style =
                MaterialTheme.typography.body2.copy(
                    color =
                        if (keepWakeLock) MaterialTheme.colors.primary
                        else MaterialTheme.colors.onSurface,
                ),
        )
      }
    }
  }
}
