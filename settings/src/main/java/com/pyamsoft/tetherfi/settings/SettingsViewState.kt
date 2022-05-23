package com.pyamsoft.tetherfi.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.pyamsoft.pydroid.arch.UiViewState
import com.pyamsoft.tetherfi.core.ActivityScope
import javax.inject.Inject

interface SettingsViewState : UiViewState {
  val topBarOffset: Int
}

@ActivityScope
internal class MutableSettingsViewState @Inject internal constructor() : SettingsViewState {
  override var topBarOffset by mutableStateOf(0)
}
