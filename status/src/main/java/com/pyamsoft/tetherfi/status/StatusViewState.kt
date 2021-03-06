package com.pyamsoft.tetherfi.status

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.pyamsoft.pydroid.arch.UiViewState
import com.pyamsoft.tetherfi.core.ActivityScope
import com.pyamsoft.tetherfi.server.ServerNetworkBand
import com.pyamsoft.tetherfi.server.status.RunningStatus
import com.pyamsoft.tetherfi.server.widi.WiDiNetworkStatus
import javax.inject.Inject

@Stable
interface StatusViewState : UiViewState {
  val preferencesLoaded: Boolean

  val group: WiDiNetworkStatus.GroupInfo?
  val wiDiStatus: RunningStatus
  val proxyStatus: RunningStatus

  val ssid: String
  val password: String
  val band: ServerNetworkBand?
  val ip: String
  val port: Int

  val keepWakeLock: Boolean

  val requiresPermissions: Boolean
  val explainPermissions: Boolean
  val isBatteryOptimizationsIgnored: Boolean

  val isBatteryInstructionExpanded: Boolean
  val isConnectionInstructionExpanded: Boolean
}

@ActivityScope
internal class MutableStatusViewState @Inject internal constructor() : StatusViewState {
  override var preferencesLoaded by mutableStateOf(false)

  override var group by mutableStateOf<WiDiNetworkStatus.GroupInfo?>(null)
  override var wiDiStatus by mutableStateOf<RunningStatus>(RunningStatus.NotRunning)
  override var proxyStatus by mutableStateOf<RunningStatus>(RunningStatus.NotRunning)

  override var ssid by mutableStateOf("")
  override var password by mutableStateOf("")
  override var band by mutableStateOf<ServerNetworkBand?>(null)
  override var ip by mutableStateOf("")
  override var port by mutableStateOf(0)

  override var keepWakeLock by mutableStateOf(false)

  override var requiresPermissions by mutableStateOf(false)
  override var explainPermissions by mutableStateOf(false)
  override var isBatteryOptimizationsIgnored by mutableStateOf(false)

  override var isBatteryInstructionExpanded by mutableStateOf(false)
  override var isConnectionInstructionExpanded by mutableStateOf(false)
}
