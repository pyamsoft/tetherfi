package com.pyamsoft.tetherfi.error

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.pyamsoft.pydroid.arch.UiViewState
import com.pyamsoft.tetherfi.core.ActivityScope
import com.pyamsoft.tetherfi.ui.ProxyEvent
import javax.inject.Inject

@Stable
interface ErrorViewState : UiViewState {
  val events: List<ProxyEvent>
}

@ActivityScope
internal class MutableErrorViewState @Inject internal constructor() : ErrorViewState {
  override var events by mutableStateOf<List<ProxyEvent>>(emptyList())
}
