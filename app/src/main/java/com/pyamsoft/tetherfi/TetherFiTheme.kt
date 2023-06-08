/*
 * Copyright 2023 pyamsoft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

  return remember(
      isDarkMode,
      primary,
      onPrimary,
      secondary,
      onSecondary,
  ) {
    if (isDarkMode)
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
}

@Composable
@CheckResult
private fun themeShapes(): Shapes {
  return remember {
    Shapes(
        // Don't use MaterialTheme here since we are defining the theme
        medium = RoundedCornerShape(16.dp),
    )
  }
}

@Composable
fun Activity.TetherFiTheme(
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
