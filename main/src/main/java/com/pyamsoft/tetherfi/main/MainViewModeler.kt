package com.pyamsoft.tetherfi.main

import android.app.Activity
import androidx.compose.runtime.saveable.SaveableStateRegistry
import com.pyamsoft.pydroid.arch.AbstractViewModeler
import com.pyamsoft.pydroid.ui.theme.Theming
import javax.inject.Inject

class MainViewModeler
@Inject
internal constructor(
    override val state: MutableMainViewState,
    private val theming: Theming,
) : AbstractViewModeler<MainViewState>(state) {

  fun handleSyncDarkTheme(activity: Activity) {
    val isDark = theming.isDarkTheme(activity)
    state.theme.value = if (isDark) Theming.Mode.DARK else Theming.Mode.LIGHT
  }

  fun handleOpenSettings() {
    state.isSettingsOpen.value = true
  }

  fun handleCloseSettings() {
    state.isSettingsOpen.value = false
  }

  override fun registerSaveState(
      registry: SaveableStateRegistry
  ): List<SaveableStateRegistry.Entry> =
      mutableListOf<SaveableStateRegistry.Entry>().apply {
        val s = state

        registry.registerProvider(KEY_THEME) { s.theme.value.name }.also { add(it) }
        registry.registerProvider(KEY_IS_SETTINGS_OPEN) { s.isSettingsOpen.value }.also { add(it) }
      }

  override fun consumeRestoredState(registry: SaveableStateRegistry) {
    val s = state
    registry
        .consumeRestored(KEY_THEME)
        ?.let { it as String }
        ?.let { Theming.Mode.valueOf(it) }
        ?.also { s.theme.value = it }

    registry
        .consumeRestored(KEY_IS_SETTINGS_OPEN)
        ?.let { it as Boolean }
        ?.also { s.isSettingsOpen.value = it }
  }

  companion object {

    private const val KEY_THEME = "theme"
    private const val KEY_IS_SETTINGS_OPEN = "is_settings_open"
  }
}
