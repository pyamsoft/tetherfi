package com.pyamsoft.tetherfi.info

import androidx.compose.runtime.Stable
import com.pyamsoft.pydroid.arch.UiViewState
import com.pyamsoft.tetherfi.core.ActivityScope
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Stable
interface InfoViewState : UiViewState {
  val isPasswordVisible: StateFlow<Boolean>
}

@Stable
@ActivityScope
class MutableInfoViewState @Inject internal constructor() : InfoViewState {
  override val isPasswordVisible = MutableStateFlow(false)
}
