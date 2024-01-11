/*
 * Copyright 2024 pyamsoft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
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
import androidx.activity.ComponentActivity
import androidx.annotation.CheckResult
import androidx.appcompat.R
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
import com.pyamsoft.pydroid.theme.PYDroidTheme
import com.pyamsoft.pydroid.theme.attributesFromCurrentTheme
import com.pyamsoft.pydroid.ui.app.LocalActivity
import com.pyamsoft.pydroid.ui.haptics.LocalHapticManager
import com.pyamsoft.pydroid.ui.haptics.rememberHapticManager
import com.pyamsoft.pydroid.ui.theme.Theming
import com.pyamsoft.pydroid.ui.uri.LocalExternalUriHandler
import com.pyamsoft.pydroid.ui.uri.rememberExternalUriHandler

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
            R.attr.colorAccent,
        )
      }
  val primary = colorResource(colors[0])
  val secondary = colorResource(colors[1])
  val black = colorResource(android.R.color.black)
  val white = colorResource(android.R.color.white)

  return remember(
      isDarkMode,
      primary,
      secondary,
      black,
      white,
  ) {
    if (isDarkMode)
        darkColors(
            primary = primary,
            onPrimary = black,
            secondary = secondary,
            onSecondary = white,
            // Must be specified for things like Switch color
            primaryVariant = primary,
            secondaryVariant = secondary,
        )
    else
        lightColors(
            primary = primary,
            onPrimary = black,
            secondary = secondary,
            onSecondary = white,
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
fun ComponentActivity.TetherFiTheme(
    theme: Theming.Mode,
    content: @Composable () -> Unit,
) {
  val self = this

  val isDarkMode = theme.getSystemDarkMode()
  val hapticManager = rememberHapticManager()
  val uriHandler = rememberExternalUriHandler()

  PYDroidTheme(
      colors = themeColors(self, isDarkMode),
      shapes = themeShapes(),
  ) {
    CompositionLocalProvider(
        // We update the LocalContentColor to match our onBackground. This allows the default
        // content color to be more appropriate to the theme background
        LocalContentColor provides MaterialTheme.colors.onBackground,

        // We provide the local haptic manager since PYDroid makes it optional
        LocalHapticManager provides hapticManager,

        // We provide the local Activity for performance optimization
        LocalActivity provides self,

        // We provide external URI handler
        LocalExternalUriHandler provides uriHandler,

        // And the content, finally
        content = content,
    )
  }
}

@Composable
@CheckResult
fun Theming.Mode.getSystemDarkMode(): Boolean {
  val self = this
  val isDarkMode =
      remember(self) {
        when (self) {
          Theming.Mode.LIGHT -> false
          Theming.Mode.DARK -> true
          Theming.Mode.SYSTEM -> null
        }
      }

  return isDarkMode ?: isSystemInDarkTheme()
}
