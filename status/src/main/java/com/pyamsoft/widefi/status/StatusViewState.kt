package com.pyamsoft.widefi.status

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.pyamsoft.pydroid.arch.UiViewState
import com.pyamsoft.widefi.server.ServerDefaults
import com.pyamsoft.widefi.server.status.RunningStatus
import com.pyamsoft.widefi.server.widi.WiDiNetwork
import javax.inject.Inject

@Stable
interface StatusViewState : UiViewState {
  val group: WiDiNetwork.GroupInfo?
  val wiDiStatus: RunningStatus
  val proxyStatus: RunningStatus
  val ip: String
  val port: Int
}

internal class MutableStatusViewState @Inject internal constructor() : StatusViewState {
  override var group by mutableStateOf<WiDiNetwork.GroupInfo?>(null)
  override var wiDiStatus by mutableStateOf<RunningStatus>(RunningStatus.NotRunning)
  override var proxyStatus by mutableStateOf<RunningStatus>(RunningStatus.NotRunning)
  override var ip by mutableStateOf(ServerDefaults.IP)
  override var port by mutableStateOf(ServerDefaults.PORT)
}
