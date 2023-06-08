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
import androidx.compose.runtime.Composable
import com.pyamsoft.pydroid.ui.app.ComposeThemeProvider
import com.pyamsoft.pydroid.ui.theme.ThemeProvider
import com.pyamsoft.pydroid.ui.theme.Theming

internal object TetherFiThemeProvider : ComposeThemeProvider {

  @Composable
  override fun Render(
      activity: Activity,
      themeProvider: ThemeProvider,
      content: @Composable () -> Unit
  ) {
    val theme = if (themeProvider.isDarkTheme()) Theming.Mode.DARK else Theming.Mode.LIGHT
    activity.TetherFiTheme(theme, content)
  }
}
