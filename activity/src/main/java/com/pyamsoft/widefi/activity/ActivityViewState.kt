package com.pyamsoft.widefi.activity

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.pyamsoft.pydroid.arch.UiViewState
import com.pyamsoft.widefi.ui.ProxyEvent
import javax.inject.Inject

@Stable
interface ActivityViewState : UiViewState {
  val events: List<ProxyEvent>
}

internal class MutableActivityViewState @Inject internal constructor() : ActivityViewState {
  override var events by mutableStateOf<List<ProxyEvent>>(emptyList())
}
