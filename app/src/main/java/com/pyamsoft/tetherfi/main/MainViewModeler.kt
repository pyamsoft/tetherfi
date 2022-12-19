package com.pyamsoft.tetherfi.main

import android.app.Activity
import com.pyamsoft.pydroid.arch.AbstractViewModeler
import com.pyamsoft.pydroid.arch.UiSavedStateReader
import com.pyamsoft.pydroid.arch.UiSavedStateWriter
import com.pyamsoft.pydroid.ui.theme.Theming
import javax.inject.Inject

internal class MainViewModeler
@Inject
internal constructor(
    private val state: MutableMainViewState,
    private val theming: Theming,
) : AbstractViewModeler<MainViewState>(state) {

  fun handleSyncDarkTheme(activity: Activity) {
    val isDark = theming.isDarkTheme(activity)
    state.theme = if (isDark) Theming.Mode.DARK else Theming.Mode.LIGHT
  }

  override fun saveState(outState: UiSavedStateWriter) {
    val s = state

    s.theme.also { theme ->
      if (theme != Theming.Mode.SYSTEM) {
        outState.put(KEY_THEME, theme.name)
      } else {
        outState.remove(KEY_THEME)
      }
    }
  }

  override fun restoreState(savedInstanceState: UiSavedStateReader) {
    val s = state

    savedInstanceState.get<String>(KEY_THEME)?.also { s.theme = Theming.Mode.valueOf(it) }
  }

  companion object {

    private const val KEY_THEME = "theme"
  }
}
