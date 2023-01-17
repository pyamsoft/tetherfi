package com.pyamsoft.tetherfi.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.pyamsoft.pydroid.ui.theme.ZeroElevation

@Composable
fun SettingsToolbar(
    modifier: Modifier = Modifier,
    onClose: () -> Unit,
) {
  Surface(
      modifier = modifier,
      elevation = ZeroElevation,
      contentColor = MaterialTheme.colors.onPrimary,
      color = MaterialTheme.colors.primary,
      shape =
          MaterialTheme.shapes.medium.copy(
              bottomStart = ZeroCornerSize,
              bottomEnd = ZeroCornerSize,
          ),
  ) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
      val contentColor = LocalContentColor.current

      TopAppBar(
          modifier = Modifier.fillMaxWidth(),
          backgroundColor = Color.Transparent,
          contentColor = contentColor,
          elevation = ZeroElevation,
          title = {
            Text(
                text = "Settings",
            )
          },
          navigationIcon = {
            IconButton(
                onClick = onClose,
            ) {
              Icon(
                  imageVector = Icons.Filled.Close,
                  contentDescription = "Close",
              )
            }
          },
      )
    }
  }
}

@Preview
@Composable
private fun PreviewSettingsToolbar() {
  SettingsToolbar(
      onClose = {},
  )
}
