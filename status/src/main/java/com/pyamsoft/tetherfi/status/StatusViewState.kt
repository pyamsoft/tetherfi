package com.pyamsoft.tetherfi.status

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.pyamsoft.pydroid.arch.UiViewState
import com.pyamsoft.tetherfi.core.ActivityScope
import com.pyamsoft.tetherfi.server.ServerNetworkBand
import com.pyamsoft.tetherfi.server.status.RunningStatus
import com.pyamsoft.tetherfi.server.widi.WiDiNetworkStatus
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Stable
interface StatusViewState : UiViewState {
  val loadingState: StateFlow<LoadingState>

  val group: StateFlow<WiDiNetworkStatus.GroupInfo?>
  val wiDiStatus: StateFlow<RunningStatus>
  val proxyStatus: StateFlow<RunningStatus>

  val ssid: StateFlow<String>
  val password: StateFlow<String>
  val ip: StateFlow<String>
  val port: StateFlow<Int>
  val band: StateFlow<ServerNetworkBand?>

  // Permissions
  val requiresPermissions: StateFlow<Boolean>
  val explainPermissions: StateFlow<Boolean>
  val hasNotificationPermission: StateFlow<Boolean>

  // Extras
  val isPasswordVisible: StateFlow<Boolean>
  val keepWakeLock: StateFlow<Boolean>
  val isBatteryOptimizationsIgnored: StateFlow<Boolean>

  val isShowingQRCodeDialog: StateFlow<Boolean>

  @Stable
  @Immutable
  enum class LoadingState {
    NONE,
    LOADING,
    DONE
  }
}

@Stable
@ActivityScope
class MutableStatusViewState @Inject internal constructor() : StatusViewState {
  override val loadingState = MutableStateFlow(StatusViewState.LoadingState.NONE)

  override val group = MutableStateFlow<WiDiNetworkStatus.GroupInfo?>(null)
  override val wiDiStatus = MutableStateFlow<RunningStatus>(RunningStatus.NotRunning)
  override val proxyStatus = MutableStateFlow<RunningStatus>(RunningStatus.NotRunning)

  override val ssid = MutableStateFlow("")
  override val password = MutableStateFlow("")
  override val ip = MutableStateFlow("")
  override val port = MutableStateFlow(0)
  override val band = MutableStateFlow<ServerNetworkBand?>(null)

  override val requiresPermissions = MutableStateFlow(false)
  override val explainPermissions = MutableStateFlow(false)
  override val hasNotificationPermission = MutableStateFlow(false)

  override val isPasswordVisible = MutableStateFlow(false)
  override val keepWakeLock = MutableStateFlow(false)
  override val isBatteryOptimizationsIgnored = MutableStateFlow(false)

  override val isShowingQRCodeDialog = MutableStateFlow(false)
}
