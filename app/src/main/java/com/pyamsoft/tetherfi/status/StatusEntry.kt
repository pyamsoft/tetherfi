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

import androidx.activity.ComponentActivity
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import com.pyamsoft.pydroid.arch.SaveStateDisposableEffect
import com.pyamsoft.pydroid.ui.inject.ComposableInjector
import com.pyamsoft.pydroid.ui.inject.rememberComposableInjector
import com.pyamsoft.pydroid.ui.util.rememberNotNull
import com.pyamsoft.tetherfi.ObjectGraph
import com.pyamsoft.tetherfi.core.ExperimentalRuntimeFlags
import com.pyamsoft.tetherfi.server.status.RunningStatus
import com.pyamsoft.tetherfi.ui.ServerViewState
import javax.inject.Inject

internal class StatusInjector : ComposableInjector() {

  @JvmField @Inject internal var viewModel: StatusViewModeler? = null

  override fun onInject(activity: ComponentActivity) {
    ObjectGraph.ActivityScope.retrieve(activity).plusStatus().create().inject(this)
  }

  override fun onDispose() {
    viewModel = null
  }
}

/** On mount hooks */
@Composable
private fun MountHooks(
    viewModel: StatusViewModeler,
) {
  SaveStateDisposableEffect(viewModel)

  LaunchedEffect(
      viewModel,
  ) {
    viewModel.bind(scope = this)
  }
}

@Composable
fun StatusEntry(
    modifier: Modifier = Modifier,
    appName: String,
    lazyListState: LazyListState,
    serverViewState: ServerViewState,
    experimentalRuntimeFlags: ExperimentalRuntimeFlags,

    // Actions
    onShowQRCode: () -> Unit,
    onRefreshConnection: () -> Unit,
    onJumpToHowTo: () -> Unit,
    onShowSlowSpeedHelp: () -> Unit,
    onToggleProxy: () -> Unit,

    // Dialogs
    onOpenNetworkError: () -> Unit,
    onOpenHotspotError: () -> Unit,
    onOpenProxyError: () -> Unit,
    onOpenBroadcastError: () -> Unit,

    // Tile
    onUpdateTile: (RunningStatus) -> Unit,

    // Error
    onEnableChangeFailed: (ServerPortTypes) -> Unit,
) {
  val component = rememberComposableInjector { StatusInjector() }
  val viewModel = rememberNotNull(component.viewModel)

  val handleToggleProxy by rememberUpdatedState(onToggleProxy)
  val handleOpenNetworkError by rememberUpdatedState(onOpenNetworkError)
  val handleOpenHotspotError by rememberUpdatedState(onOpenHotspotError)
  val handleOpenBroadcastError by rememberUpdatedState(onOpenBroadcastError)
  val handleOpenProxyError by rememberUpdatedState(onOpenProxyError)

  // Hooks that run on mount
  MountHooks(
      viewModel = viewModel,
  )

  StatusScreen(
      modifier = modifier,
      state = viewModel,
      lazyListState = lazyListState,
      serverViewState = serverViewState,
      appName = appName,
      experimentalRuntimeFlags = experimentalRuntimeFlags,
      onShowQRCode = onShowQRCode,
      onRefreshConnection = onRefreshConnection,
      onJumpToHowTo = onJumpToHowTo,
      onEnableChangeFailed = onEnableChangeFailed,
      onToggleProxy = {
        viewModel.handleToggleProxy(
            onToggleProxy = handleToggleProxy,
        )
      },
      onSsidChanged = { viewModel.handleSsidChanged(it.trim()) },
      onPasswordChanged = { viewModel.handlePasswordChanged(it) },
      onHttpEnabledChanged = { viewModel.handleEnabledChanged(it, ServerPortTypes.HTTP) },
      onHttpPortChanged = { viewModel.handlePortChanged(it, ServerPortTypes.HTTP) },
      onSocksEnabledChanged = { viewModel.handleEnabledChanged(it, ServerPortTypes.SOCKS) },
      onSocksPortChanged = { viewModel.handlePortChanged(it, ServerPortTypes.SOCKS) },
      onViewSlowSpeedHelp = onShowSlowSpeedHelp,
      onSelectBand = { viewModel.handleChangeBand(it) },
      onStatusUpdated = onUpdateTile,
      onTogglePasswordVisibility = { viewModel.handleTogglePasswordVisibility() },
      onShowNetworkError = { handleOpenNetworkError() },
      onShowHotspotError = { handleOpenHotspotError() },
      onShowProxyError = { handleOpenProxyError() },
      onShowBroadcastError = { handleOpenBroadcastError() },
      onSelectBroadcastType = { viewModel.handleUpdateBroadcastType(it) },
      onSelectPreferredNetwork = { viewModel.handleUpdatePreferredNetwork(it) },
  )
}
