package com.pyamsoft.tetherfi.main

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.pyamsoft.pydroid.arch.UiViewState
import com.pyamsoft.pydroid.ui.theme.Theming
import javax.inject.Inject

@Stable
interface MainViewState : UiViewState {
  val theme: Theming.Mode
}

internal class MutableMainViewState @Inject internal constructor() : MainViewState {
  override var theme by mutableStateOf(Theming.Mode.SYSTEM)
}
