package com.pyamsoft.tetherfi.ui

import androidx.compose.runtime.Stable
import com.pyamsoft.pydroid.arch.UiViewState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Activity global view state for server variables */
@Stable
interface ServerViewState : UiViewState {
  val ssid: StateFlow<String>
  val password: StateFlow<String>
  val ip: StateFlow<String>
  val port: StateFlow<Int>
}

/** Only for testing */
@Stable
class TestServerViewState : ServerViewState {
  override val ssid = MutableStateFlow("MySSID")
  override val password = MutableStateFlow("MyPassword")
  override val ip = MutableStateFlow("192.168.49.1")
  override val port = MutableStateFlow(8228)
}
