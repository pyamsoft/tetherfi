package com.pyamsoft.tetherfi

import android.app.Activity
import androidx.compose.runtime.Composable
import com.pyamsoft.pydroid.ui.app.ComposeThemeProvider
import com.pyamsoft.pydroid.ui.theme.ThemeProvider

internal object TetherFiThemeProvider : ComposeThemeProvider {

  @Composable
  override fun Render(
      activity: Activity,
      themeProvider: ThemeProvider,
      content: @Composable () -> Unit
  ) {
    activity.TetherFiTheme(themeProvider, content)
  }
}
