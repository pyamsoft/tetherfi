package com.pyamsoft.tetherfi.status

import androidx.compose.foundation.layout.Column
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.pyamsoft.tetherfi.ui.Label

@Composable
internal fun StatusItem(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    valueStyle: TextStyle = MaterialTheme.typography.body1,
    color: Color = Color.Unspecified,
) {
  Column(
      modifier = modifier,
  ) {
    Label(
        text = title,
        color = MaterialTheme.colors.onSurface,
    )
    Text(
        text = value,
        style = valueStyle,
        color = color,
    )
  }
}
