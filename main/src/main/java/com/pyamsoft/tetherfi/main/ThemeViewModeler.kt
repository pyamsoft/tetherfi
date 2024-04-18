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

import android.app.Activity
import android.content.res.Configuration
import androidx.compose.runtime.saveable.SaveableStateRegistry
import com.pyamsoft.pydroid.arch.AbstractViewModeler
import com.pyamsoft.pydroid.ui.theme.Theming
import javax.inject.Inject

class ThemeViewModeler
@Inject
internal constructor(
    override val state: MutableThemeViewState,
    private val theming: Theming,
) : ThemeViewState by state, AbstractViewModeler<ThemeViewState>(state) {

  override fun registerSaveState(
      registry: SaveableStateRegistry
  ): List<SaveableStateRegistry.Entry> =
      mutableListOf<SaveableStateRegistry.Entry>().apply {
        val s = state

        registry.registerProvider(KEY_THEME) { s.theme.value.name }.also { add(it) }
      }

  override fun consumeRestoredState(registry: SaveableStateRegistry) {
    val s = state
    registry
        .consumeRestored(KEY_THEME)
        ?.let { it as String }
        ?.let { Theming.Mode.valueOf(it) }
        ?.also { s.theme.value = it }
  }

  fun handleSyncDarkTheme(activity: Activity) {
    handleSyncDarkTheme(activity.resources.configuration)
  }

  fun handleSyncDarkTheme(configuration: Configuration) {
    val isDark = theming.isDarkTheme(configuration)
    state.theme.value = if (isDark) Theming.Mode.DARK else Theming.Mode.LIGHT
  }

  companion object {

    private const val KEY_THEME = "theme"
  }
}
