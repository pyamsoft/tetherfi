/*
 * Copyright 2024 pyamsoft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pyamsoft.tetherfi.status

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.pyamsoft.pydroid.arch.UiViewState
import com.pyamsoft.tetherfi.server.ServerNetworkBand
import com.pyamsoft.tetherfi.server.ServerPerformanceLimit
import com.pyamsoft.tetherfi.server.status.RunningStatus
import com.pyamsoft.tetherfi.service.prereq.HotspotStartBlocker
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Stable
interface StatusViewState : UiViewState {

  val loadingState: StateFlow<LoadingState>

  val wiDiStatus: StateFlow<RunningStatus>
  val proxyStatus: StateFlow<RunningStatus>

  // For editing, at proxy runtime we pull from ServerViewState
  val ssid: StateFlow<String>
  val password: StateFlow<String>
  val isPasswordVisible: StateFlow<Boolean>
  val port: StateFlow<String>
  val band: StateFlow<ServerNetworkBand?>

  // Operating Settings
  val hasNotificationPermission: StateFlow<Boolean>
  val isBatteryOptimizationsIgnored: StateFlow<Boolean>

  // Tweaks
  val isIgnoreVpn: StateFlow<Boolean>
  val isIgnoreLocation: StateFlow<Boolean>
  val isShutdownWithNoClients: StateFlow<Boolean>
  val isKeepScreenOn: StateFlow<Boolean>

  // Expert
  val powerBalance: StateFlow<ServerPerformanceLimit>

  // Dialogs
  val startBlockers: StateFlow<Collection<HotspotStartBlocker>>
  val isShowingSetupError: StateFlow<Boolean>
  val isShowingNetworkError: StateFlow<Boolean>
  val isShowingHotspotError: StateFlow<Boolean>
  val isShowingBroadcastError: StateFlow<Boolean>
  val isShowingProxyError: StateFlow<Boolean>
  val isShowingPowerBalance: StateFlow<Boolean>

  @Stable
  @Immutable
  enum class LoadingState {
    NONE,
    LOADING,
    DONE
  }
}

@Stable
class MutableStatusViewState @Inject internal constructor() : StatusViewState {
  override val loadingState = MutableStateFlow(StatusViewState.LoadingState.NONE)

  override val wiDiStatus = MutableStateFlow<RunningStatus>(RunningStatus.NotRunning)
  override val proxyStatus = MutableStateFlow<RunningStatus>(RunningStatus.NotRunning)

  override val ssid = MutableStateFlow("")
  override val password = MutableStateFlow("")
  override val isPasswordVisible = MutableStateFlow(false)
  override val port = MutableStateFlow("")
  override val band = MutableStateFlow<ServerNetworkBand?>(null)

  override val startBlockers = MutableStateFlow<Collection<HotspotStartBlocker>>(emptySet())
  override val hasNotificationPermission = MutableStateFlow(false)

  override val isBatteryOptimizationsIgnored = MutableStateFlow(false)
  override val powerBalance =
      MutableStateFlow<ServerPerformanceLimit>(ServerPerformanceLimit.Defaults.BOUND_N_CPU)

  override val isShowingSetupError = MutableStateFlow(false)
  override val isShowingNetworkError = MutableStateFlow(false)
  override val isShowingHotspotError = MutableStateFlow(false)
  override val isShowingBroadcastError = MutableStateFlow(false)
  override val isShowingProxyError = MutableStateFlow(false)
  override val isShowingPowerBalance = MutableStateFlow(false)

  override val isIgnoreVpn = MutableStateFlow(false)
  override val isIgnoreLocation = MutableStateFlow(false)
  override val isShutdownWithNoClients = MutableStateFlow(false)
  override val isKeepScreenOn = MutableStateFlow(false)
}
