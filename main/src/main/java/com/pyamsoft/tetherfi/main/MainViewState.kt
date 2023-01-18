package com.pyamsoft.tetherfi.main

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.pyamsoft.pydroid.arch.UiViewState
import com.pyamsoft.pydroid.ui.theme.Theming
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Stable
interface MainViewState : UiViewState {
  val theme: StateFlow<Theming.Mode>
  val isSettingsOpen: StateFlow<Boolean>
}

@Stable
internal class MutableMainViewState @Inject internal constructor() : MainViewState {
  override val theme = MutableStateFlow(Theming.Mode.SYSTEM)
  override val isSettingsOpen = MutableStateFlow(false)
}
