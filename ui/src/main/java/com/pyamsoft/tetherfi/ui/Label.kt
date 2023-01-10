package com.pyamsoft.tetherfi.ui

import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

@Composable
fun Label(
    modifier: Modifier = Modifier,
    text: String,
    color: Color = MaterialTheme.colors.onBackground,
) {
  Text(
      modifier = modifier,
      text = text,
      style =
          MaterialTheme.typography.caption.copy(
              fontWeight = FontWeight.W700,
              color =
                  color.copy(
                      alpha = ContentAlpha.medium,
                  ),
          ),
  )
}
