package com.pyamsoft.tetherfi.status

import androidx.compose.runtime.Immutable
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

interface StatusViewState : UiViewState {
  val loadingState: LoadingState

  val group: WiDiNetworkStatus.GroupInfo?
  val wiDiStatus: RunningStatus
  val proxyStatus: RunningStatus

  val ssid: String
  val password: String
  val ip: String
  val port: Int
  val band: ServerNetworkBand?

  // Permissions
  val requiresPermissions: Boolean
  val explainPermissions: Boolean

  // Extras
  val keepWakeLock: Boolean
  val isBatteryOptimizationsIgnored: Boolean

  @Stable
  @Immutable
  enum class LoadingState {
    NONE,
    LOADING,
    DONE
  }
}

@ActivityScope
internal class MutableStatusViewState @Inject internal constructor() : StatusViewState {
  override var loadingState by mutableStateOf(StatusViewState.LoadingState.NONE)

  override var group by mutableStateOf<WiDiNetworkStatus.GroupInfo?>(null)
  override var wiDiStatus by mutableStateOf<RunningStatus>(RunningStatus.NotRunning)
  override var proxyStatus by mutableStateOf<RunningStatus>(RunningStatus.NotRunning)

  override var ssid by mutableStateOf("")
  override var password by mutableStateOf("")
  override var ip by mutableStateOf("")
  override var port by mutableStateOf(0)
  override var band by mutableStateOf<ServerNetworkBand?>(null)

  override var requiresPermissions by mutableStateOf(false)
  override var explainPermissions by mutableStateOf(false)

  override var keepWakeLock by mutableStateOf(false)
  override var isBatteryOptimizationsIgnored by mutableStateOf(false)

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as MutableStatusViewState

    if (loadingState != other.loadingState) return false
    if (group != other.group) return false
    if (wiDiStatus != other.wiDiStatus) return false
    if (proxyStatus != other.proxyStatus) return false
    if (ssid != other.ssid) return false
    if (password != other.password) return false
    if (ip != other.ip) return false
    if (port != other.port) return false
    if (band != other.band) return false
    if (requiresPermissions != other.requiresPermissions) return false
    if (explainPermissions != other.explainPermissions) return false
    if (keepWakeLock != other.keepWakeLock) return false
    if (isBatteryOptimizationsIgnored != other.isBatteryOptimizationsIgnored) return false

    return true
  }

  override fun hashCode(): Int {
    var result = loadingState.hashCode()
    result = 31 * result + (group?.hashCode() ?: 0)
    result = 31 * result + wiDiStatus.hashCode()
    result = 31 * result + proxyStatus.hashCode()
    result = 31 * result + ssid.hashCode()
    result = 31 * result + password.hashCode()
    result = 31 * result + ip.hashCode()
    result = 31 * result + port
    result = 31 * result + (band?.hashCode() ?: 0)
    result = 31 * result + requiresPermissions.hashCode()
    result = 31 * result + explainPermissions.hashCode()
    result = 31 * result + keepWakeLock.hashCode()
    result = 31 * result + isBatteryOptimizationsIgnored.hashCode()
    return result
  }
}
