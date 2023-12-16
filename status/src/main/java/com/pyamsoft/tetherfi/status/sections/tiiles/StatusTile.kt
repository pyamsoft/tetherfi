package com.pyamsoft.tetherfi.status.sections.tiiles

import androidx.compose.foundation.border
import androidx.compose.material.Card
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pyamsoft.pydroid.ui.defaults.CardDefaults

@Composable
internal fun StatusTile(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colors.primary,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
  Card(
      modifier =
          modifier.border(
              width = 2.dp,
              color =
                  color.copy(
                      alpha = if (enabled) ContentAlpha.medium else ContentAlpha.disabled,
                  ),
              shape = MaterialTheme.shapes.medium,
          ),
      shape = MaterialTheme.shapes.medium,
      elevation = CardDefaults.Elevation,
  ) {
    content()
  }
}
