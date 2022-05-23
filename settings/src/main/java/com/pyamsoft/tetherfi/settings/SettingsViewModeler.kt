package com.pyamsoft.tetherfi.settings

import com.pyamsoft.pydroid.arch.AbstractViewModeler
import com.pyamsoft.pydroid.arch.UiSavedStateReader
import com.pyamsoft.pydroid.arch.UiSavedStateWriter
import javax.inject.Inject

class SettingsViewModeler
@Inject
internal constructor(
    private val state: MutableSettingsViewState,
) : AbstractViewModeler<SettingsViewState>(state) {

  fun handleTopBarHeight(height: Int) {
    state.topBarOffset = height
  }

  override fun saveState(outState: UiSavedStateWriter) {
    state.topBarOffset.also { offset ->
      if (offset > 0) {
        outState.put(KEY_TOP_OFFSET, offset)
      } else {
        outState.remove(KEY_TOP_OFFSET)
      }
    }
  }

  override fun restoreState(savedInstanceState: UiSavedStateReader) {
    savedInstanceState.get<Int>(KEY_TOP_OFFSET)?.also { offset -> state.topBarOffset = offset }
  }

  companion object {

    private const val KEY_TOP_OFFSET = "top_offset"
  }
}
