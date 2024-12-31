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

package com.pyamsoft.tetherfi.behavior

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pyamsoft.tetherfi.behavior.sections.expert.PowerBalanceDialog
import com.pyamsoft.tetherfi.behavior.sections.expert.SocketTimeoutDialog
import com.pyamsoft.tetherfi.server.ServerPerformanceLimit
import com.pyamsoft.tetherfi.server.ServerSocketTimeout

@Composable
internal fun BehaviorDialogs(
    dialogModifier: Modifier = Modifier,
    state: BehaviorViewState,

    // Power Balance
    onHidePowerBalance: () -> Unit,
    onUpdatePowerBalance: (ServerPerformanceLimit) -> Unit,

    // Socket Timeout
    onHideSocketTimeout: () -> Unit,
    onUpdateSocketTimeout: (ServerSocketTimeout) -> Unit,
) {

  val isShowingPowerBalance by state.isShowingPowerBalance.collectAsStateWithLifecycle()
  val isShowingSocketTimeout by state.isShowingSocketTimeout.collectAsStateWithLifecycle()

  AnimatedVisibility(
      visible = isShowingPowerBalance,
  ) {
    PowerBalanceDialog(
        modifier = dialogModifier,
        state = state,
        onHidePowerBalance = onHidePowerBalance,
        onUpdatePowerBalance = onUpdatePowerBalance,
    )
  }

  AnimatedVisibility(
      visible = isShowingSocketTimeout,
  ) {
    SocketTimeoutDialog(
        modifier = dialogModifier,
        state = state,
        onHideSocketTimeout = onHideSocketTimeout,
        onUpdateSocketTimeout = onUpdateSocketTimeout,
    )
  }
}
