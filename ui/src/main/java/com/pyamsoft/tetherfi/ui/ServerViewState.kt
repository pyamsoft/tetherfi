package com.pyamsoft.tetherfi.ui

import androidx.compose.runtime.Stable
import com.pyamsoft.pydroid.arch.UiViewState
import com.pyamsoft.tetherfi.server.ServerDefaults
import com.pyamsoft.tetherfi.server.widi.WiDiNetworkStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Activity global view state for server variables */
@Stable
interface ServerViewState : UiViewState {
  val group: StateFlow<WiDiNetworkStatus.GroupInfo>
  val connection: StateFlow<WiDiNetworkStatus.ConnectionInfo>
  val port: StateFlow<Int>
}

/** Only for testing */
@Stable
class TestServerViewState : ServerViewState {
  override val group =
      MutableStateFlow<WiDiNetworkStatus.GroupInfo>(WiDiNetworkStatus.GroupInfo.Empty)
  override val connection =
      MutableStateFlow<WiDiNetworkStatus.ConnectionInfo>(WiDiNetworkStatus.ConnectionInfo.Empty)
  override val port = MutableStateFlow(ServerDefaults.PORT)
}
