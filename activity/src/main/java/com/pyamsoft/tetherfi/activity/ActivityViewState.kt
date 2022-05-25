package com.pyamsoft.tetherfi.activity

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.pyamsoft.pydroid.arch.UiViewState
import com.pyamsoft.tetherfi.core.ActivityScope
import com.pyamsoft.tetherfi.ui.ProxyEvent
import javax.inject.Inject

@Stable
interface ActivityViewState : UiViewState {
  val events: List<ProxyEvent>
}

@ActivityScope
internal class MutableActivityViewState @Inject internal constructor() : ActivityViewState {
  override var events by mutableStateOf<List<ProxyEvent>>(emptyList())
}
