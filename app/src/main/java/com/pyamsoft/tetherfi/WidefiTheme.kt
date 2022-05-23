package com.pyamsoft.tetherfi

import android.app.Activity
import androidx.annotation.CheckResult
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Colors
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Shapes
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.google.android.material.R
import com.pyamsoft.pydroid.theme.PYDroidTheme
import com.pyamsoft.pydroid.theme.attributesFromCurrentTheme
import com.pyamsoft.pydroid.ui.theme.ThemeProvider
import com.pyamsoft.pydroid.ui.theme.Theming

@Composable
@CheckResult
private fun themeColors(
    activity: Activity,
    isDarkMode: Boolean,
): Colors {
  val colors =
      remember(isDarkMode) {
        activity.attributesFromCurrentTheme(
            R.attr.colorPrimary,
            R.attr.colorOnPrimary,
            R.attr.colorSecondary,
            R.attr.colorOnSecondary,
        )
      }
  val primary = colorResource(colors[0])
  val onPrimary = colorResource(colors[1])
  val secondary = colorResource(colors[2])
  val onSecondary = colorResource(colors[3])
  return if (isDarkMode)
      darkColors(
          primary = primary,
          onPrimary = onPrimary,
          secondary = secondary,
          onSecondary = onSecondary,
          // Must be specified for things like Switch color
          primaryVariant = primary,
          secondaryVariant = secondary,
      )
  else
      lightColors(
          primary = primary,
          onPrimary = onPrimary,
          secondary = secondary,
          onSecondary = onSecondary,
          // Must be specified for things like Switch color
          primaryVariant = primary,
          secondaryVariant = secondary,
      )
}

@Composable
@CheckResult
private fun themeShapes(): Shapes {
  return Shapes(
      // Don't use MaterialTheme here since we are defining the theme
      medium = RoundedCornerShape(16.dp),
  )
}

@Composable
fun Activity.WidefiTheme(
    themeProvider: ThemeProvider,
    content: @Composable () -> Unit,
) {
  this.WidefiTheme(
      theme = if (themeProvider.isDarkTheme()) Theming.Mode.DARK else Theming.Mode.LIGHT,
      content = content,
  )
}

@Composable
fun Activity.WidefiTheme(
    theme: Theming.Mode,
    content: @Composable () -> Unit,
) {
  val isDarkMode =
      when (theme) {
        Theming.Mode.LIGHT -> false
        Theming.Mode.DARK -> true
        Theming.Mode.SYSTEM -> isSystemInDarkTheme()
      }

  PYDroidTheme(
      colors = themeColors(this, isDarkMode),
      shapes = themeShapes(),
  ) {
    // We update the LocalContentColor to match our onBackground. This allows the default
    // content color to be more appropriate to the theme background
    CompositionLocalProvider(
        LocalContentColor provides MaterialTheme.colors.onBackground,
        content = content,
    )
  }
}
