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

import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.lifecycle.lifecycleScope
import com.pyamsoft.pydroid.arch.AbstractViewModeler
import com.pyamsoft.pydroid.ui.theme.Theming
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
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

            registry.registerProvider(KEY_THEME) {
                val current = s.theme.value
                return@registerProvider "${current.mode}|${current.isMaterialYou}"
            }.also { add(it) }
        }

    override fun consumeRestoredState(registry: SaveableStateRegistry) {
        val s = state
        registry
            .consumeRestored(KEY_THEME)
            ?.let { it as String }
            ?.also { current ->
                val split = current.split("|")
                val mode = Theming.Mode.valueOf(split[0])
                val isMaterialYou = split[1].toBooleanStrict()
                s.theme.value = ThemeSnapshot(
                    mode = mode,
                    isMaterialYou = isMaterialYou,
                )
            }
    }

    private fun bind(scope: CoroutineScope) {
        combineTransform(
            theming.listenForModeChanges(),
            theming.listenForMaterialYouChanges(),
        ) { mode, isMaterialYou ->
            emit(mode to isMaterialYou)
        }.flowOn(context = Dispatchers.Default)
            .also { f ->
                scope.launch(context = Dispatchers.Default) {
                    f.collect {
                        state.theme.value = ThemeSnapshot(it.first, it.second)
                    }
                }
            }
    }

    private fun handleSyncDarkTheme(activity: ComponentActivity) {
        handleSyncDarkTheme(
            scope = activity.lifecycleScope, configuration = activity.resources.configuration,
        )
    }

    fun init(activity: ComponentActivity) {
        bind(scope = activity.lifecycleScope)
        handleSyncDarkTheme(activity)
    }

    fun handleSyncDarkTheme(
        scope: CoroutineScope,
        configuration: Configuration,
    ) {
        val isDark = theming.isDarkTheme(configuration)
        val newMode = if (isDark) Theming.Mode.DARK else Theming.Mode.LIGHT
        theming.setDarkTheme(scope, newMode)
    }

    companion object {

        private const val KEY_THEME = "theme"
    }
}
