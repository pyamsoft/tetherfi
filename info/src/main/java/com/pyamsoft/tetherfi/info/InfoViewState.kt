package com.pyamsoft.tetherfi.info

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.pyamsoft.pydroid.arch.UiViewState
import com.pyamsoft.tetherfi.core.ActivityScope
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Stable
interface InfoViewState : UiViewState {
  val ssid: StateFlow<String>
  val password: StateFlow<String>
  val ip: StateFlow<String>
  val port: StateFlow<Int>
}

@Stable
@ActivityScope
class MutableInfoViewState @Inject internal constructor() : InfoViewState {
  override val ssid = MutableStateFlow("")
  override val password = MutableStateFlow("")
  override val ip = MutableStateFlow("")
  override val port = MutableStateFlow(0)
}
