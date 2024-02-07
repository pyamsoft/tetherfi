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

package com.pyamsoft.tetherfi.main

import android.graphics.Color
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.annotation.CheckResult
import androidx.annotation.ColorInt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsControllerCompat

private val LIGHT_MODE_SCRIM by lazy { Color.argb(0x60, 0x1B, 0x1B, 0x1B) }

/** Android below O has zero support for navbar color, the appearance method is a lie. */
@CheckResult
private fun needsNavbarScrim(): Boolean {
  return Build.VERSION.SDK_INT < Build.VERSION_CODES.O
}

@ColorInt
@CheckResult
private fun getLightModeColor(): Int {
  return if (needsNavbarScrim()) LIGHT_MODE_SCRIM else Color.TRANSPARENT
}

@Composable
fun ComponentActivity.SystemBars(
    isDarkMode: Boolean,
) {
  val view = LocalView.current
  val w = window
  val controller =
      remember(
          w,
          view,
      ) {
        WindowInsetsControllerCompat(w, view)
      }
  LaunchedEffect(
      isDarkMode,
      controller,
  ) {
    val statusStyle =
        if (isDarkMode) SystemBarStyle.dark(Color.TRANSPARENT)
        else SystemBarStyle.light(Color.TRANSPARENT, getLightModeColor())

    val navStyle =
        if (isDarkMode) SystemBarStyle.dark(Color.TRANSPARENT)
        else SystemBarStyle.light(Color.TRANSPARENT, getLightModeColor())
    enableEdgeToEdge(
        statusBarStyle = statusStyle,
        navigationBarStyle = navStyle,
    )
    controller.isAppearanceLightStatusBars = true
    controller.isAppearanceLightNavigationBars = isDarkMode
  }
}
