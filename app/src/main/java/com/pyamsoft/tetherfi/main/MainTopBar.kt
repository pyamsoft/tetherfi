package com.pyamsoft.tetherfi.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.pyamsoft.pydroid.ui.theme.ZeroElevation
import com.pyamsoft.tetherfi.R

@JvmOverloads
@Composable
internal fun MainTopBar(
    modifier: Modifier = Modifier,
    onSettingsOpen: () -> Unit,
) {
  Surface(
      modifier = modifier,
      color = MaterialTheme.colors.background,
      elevation = ZeroElevation,
  ) {
    Surface(
        contentColor = MaterialTheme.colors.onPrimary,
        color = MaterialTheme.colors.primary,
        shape =
            MaterialTheme.shapes.medium.copy(
                topStart = ZeroCornerSize,
                topEnd = ZeroCornerSize,
            ),
        elevation = AppBarDefaults.TopAppBarElevation,
    ) {
      Column {
        TopAppBar(
            modifier = Modifier.statusBarsPadding(),
            elevation = ZeroElevation,
            backgroundColor = Color.Transparent,
            contentColor = LocalContentColor.current,
            title = {
              Text(
                  text = stringResource(R.string.app_name),
              )
            },
            actions = {
              IconButton(
                  onClick = onSettingsOpen,
              ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Open Settings",
                )
              }
            },
        )
      }
    }
  }
}
