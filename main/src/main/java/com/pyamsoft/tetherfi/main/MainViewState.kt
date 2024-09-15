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

package com.pyamsoft.tetherfi.main

import androidx.compose.runtime.Stable
import com.pyamsoft.tetherfi.core.ActivityScope
import com.pyamsoft.tetherfi.server.broadcast.BroadcastNetworkStatus
import com.pyamsoft.tetherfi.ui.ServerViewState
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Stable
interface MainViewState : ServerViewState {
  val isSettingsOpen: StateFlow<Boolean>
  val isShowingQRCodeDialog: StateFlow<Boolean>
  val isShowingSlowSpeedHelp: StateFlow<Boolean>
}

@Stable
@ActivityScope
class MutableMainViewState @Inject internal constructor() : MainViewState {
  override val group =
      MutableStateFlow<BroadcastNetworkStatus.GroupInfo>(BroadcastNetworkStatus.GroupInfo.Empty)
  override val connection =
      MutableStateFlow<BroadcastNetworkStatus.ConnectionInfo>(
          BroadcastNetworkStatus.ConnectionInfo.Empty)
  override val port = MutableStateFlow(0)

  // TODO RNDIS via preference
  override val isRNDISConnection = MutableStateFlow(true)

  override val isSettingsOpen = MutableStateFlow(false)

  override val isShowingQRCodeDialog = MutableStateFlow(false)
  override val isShowingSlowSpeedHelp = MutableStateFlow(false)
}
