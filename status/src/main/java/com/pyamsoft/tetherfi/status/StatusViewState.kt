package com.pyamsoft.tetherfi.status

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.pyamsoft.pydroid.arch.UiViewState
import com.pyamsoft.tetherfi.server.ServerNetworkBand
import com.pyamsoft.tetherfi.server.status.RunningStatus
import com.pyamsoft.tetherfi.server.widi.WiDiNetwork
import javax.inject.Inject

@Stable
interface StatusViewState : UiViewState {
  val group: WiDiNetwork.GroupInfo?
  val wiDiStatus: RunningStatus
  val proxyStatus: RunningStatus

  val preferencesLoaded: Boolean
  val ssid: String
  val password: String
  val band: ServerNetworkBand?
  val ip: String
  val port: Int

  val isBatteryOptimizationsIgnored: Boolean
}

internal class MutableStatusViewState @Inject internal constructor() : StatusViewState {
  override var group by mutableStateOf<WiDiNetwork.GroupInfo?>(null)
  override var wiDiStatus by mutableStateOf<RunningStatus>(RunningStatus.NotRunning)
  override var proxyStatus by mutableStateOf<RunningStatus>(RunningStatus.NotRunning)
  override var preferencesLoaded by mutableStateOf(false)
  override var ssid by mutableStateOf("")
  override var password by mutableStateOf("")
  override var band by mutableStateOf<ServerNetworkBand?>(null)
  override var ip by mutableStateOf("")
  override var port by mutableStateOf(0)
  override var isBatteryOptimizationsIgnored by mutableStateOf(false)
}
