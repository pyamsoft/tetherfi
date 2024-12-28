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

import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.pyamsoft.pydroid.arch.SaveStateDisposableEffect
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.bus.EventConsumer
import com.pyamsoft.pydroid.ui.inject.ComposableInjector
import com.pyamsoft.pydroid.ui.inject.rememberComposableInjector
import com.pyamsoft.pydroid.ui.util.rememberNotNull
import com.pyamsoft.tetherfi.ObjectGraph
import com.pyamsoft.tetherfi.server.status.RunningStatus
import com.pyamsoft.tetherfi.ui.ServerViewState
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

internal class StatusInjector : ComposableInjector() {

  @JvmField @Inject internal var viewModel: StatusViewModeler? = null

  @JvmField @Inject internal var permissionRequestBus: EventBus<PermissionRequests>? = null

  @JvmField @Inject internal var permissionResponseBus: EventConsumer<PermissionResponse>? = null

  override fun onInject(activity: ComponentActivity) {
    ObjectGraph.ActivityScope.retrieve(activity).plusStatus().create().inject(this)
  }

  override fun onDispose() {
    viewModel = null
    permissionRequestBus = null
    permissionResponseBus = null
  }
}

/** Sets up permission request interaction */
@Composable
private fun RegisterPermissionRequests(
    permissionResponseBus: Flow<PermissionResponse>,
    onRefreshSystemInfo: CoroutineScope.() -> Unit,
) {
  // Create requesters
  val handleRefreshSystemInfo by rememberUpdatedState(onRefreshSystemInfo)

  LaunchedEffect(
      permissionResponseBus,
  ) {
    // See MainActivity
    permissionResponseBus.flowOn(context = Dispatchers.Default).also { f ->
      launch(context = Dispatchers.Default) {
        f.collect { resp ->
          when (resp) {
            is PermissionResponse.RefreshNotification -> {
              // Call to the VM to refresh info
              handleRefreshSystemInfo(this)
            }
            is PermissionResponse.ToggleProxy -> {
              // Blank
            }
          }
        }
      }
    }
  }
}

/** On mount hooks */
@Composable
private fun MountHooks(
    viewModel: StatusViewModeler,
    permissionResponseBus: Flow<PermissionResponse>,
    onRefreshConnection: () -> Unit
) {
  val scope = rememberCoroutineScope()

  val handleRefreshSystemInfo by rememberUpdatedState { s: CoroutineScope ->
    viewModel.handleRefreshSystemInfo(scope = s)
  }

  // As early as possible because of Lifecycle quirks
  RegisterPermissionRequests(
      permissionResponseBus = permissionResponseBus,
      onRefreshSystemInfo = { handleRefreshSystemInfo(this) },
  )

  SaveStateDisposableEffect(viewModel)

  LaunchedEffect(
      viewModel,
  ) {
    viewModel.bind(scope = this)
  }

  val handleRefreshConnectionInfo by rememberUpdatedState { onRefreshConnection() }
  val bindLifecycleResumed by rememberUpdatedState {
    viewModel.bindLifecycleResumed(
        scope = scope,
        onRefreshConnectionInfo = { handleRefreshConnectionInfo() },
    )
  }
  LifecycleEventEffect(
      event = Lifecycle.Event.ON_RESUME,
  ) {
    bindLifecycleResumed()
  }
}

@Composable
fun StatusEntry(
    modifier: Modifier = Modifier,
    appName: String,
    serverViewState: ServerViewState,

    // Actions
    onShowQRCode: () -> Unit,
    onRefreshConnection: () -> Unit,
    onJumpToHowTo: () -> Unit,
    onLaunchIntent: (String) -> Unit,
    onShowSlowSpeedHelp: () -> Unit,
    onToggleProxy: () -> Unit,

    // Dialogs
    onOpenNetworkError: () -> Unit,
    onOpenHotspotError: () -> Unit,
    onOpenProxyError: () -> Unit,
    onOpenBroadcastError: () -> Unit,

    // Tile
    onUpdateTile: (RunningStatus) -> Unit,
) {
  val component = rememberComposableInjector { StatusInjector() }
  val viewModel = rememberNotNull(component.viewModel)
  val permissionRequestBus = rememberNotNull(component.permissionRequestBus)
  val permissionResponseBus = rememberNotNull(component.permissionResponseBus)

  // Use the LifecycleOwner.CoroutineScope (Activity usually)
  // so that the scope does not die because of navigation events
  val owner = LocalLifecycleOwner.current
  val lifecycleScope = owner.lifecycleScope

  val handleToggleProxy by rememberUpdatedState(onToggleProxy)
  val handleOpenNetworkError by rememberUpdatedState(onOpenNetworkError)
  val handleOpenHotspotError by rememberUpdatedState(onOpenHotspotError)
  val handleOpenBroadcastError by rememberUpdatedState(onOpenBroadcastError)
  val handleOpenProxyError by rememberUpdatedState(onOpenProxyError)

  // Hooks that run on mount
  MountHooks(
      viewModel = viewModel,
      permissionResponseBus = permissionResponseBus,
      onRefreshConnection = onRefreshConnection,
  )

  StatusScreen(
      modifier = modifier,
      state = viewModel,
      serverViewState = serverViewState,
      appName = appName,
      onShowQRCode = onShowQRCode,
      onRefreshConnection = onRefreshConnection,
      onJumpToHowTo = onJumpToHowTo,
      onToggleProxy = {
        viewModel.handleToggleProxy(
            onToggleProxy = handleToggleProxy,
        )
      },
      onSsidChanged = { viewModel.handleSsidChanged(it.trim()) },
      onPasswordChanged = { viewModel.handlePasswordChanged(it) },
      onPortChanged = { viewModel.handlePortChanged(it) },
      onViewSlowSpeedHelp = onShowSlowSpeedHelp,
      onOpenBatterySettings = {
        onLaunchIntent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
      },
      onSelectBand = { viewModel.handleChangeBand(it) },
      onRequestNotificationPermission = {
        lifecycleScope.launch(context = Dispatchers.Default) {
          // See MainActivity
          permissionRequestBus.emit(PermissionRequests.Notification)
        }
      },
      onStatusUpdated = onUpdateTile,
      onTogglePasswordVisibility = { viewModel.handleTogglePasswordVisibility() },
      onShowNetworkError = { handleOpenNetworkError() },
      onShowHotspotError = { handleOpenHotspotError() },
      onToggleIgnoreVpn = { viewModel.handleToggleTweak(StatusViewTweaks.IGNORE_VPN) },
      onToggleIgnoreLocation = { viewModel.handleToggleTweak(StatusViewTweaks.IGNORE_LOCATION) },
      onToggleShutdownWithNoClients = {
        viewModel.handleToggleTweak(StatusViewTweaks.SHUTDOWN_NO_CLIENTS)
      },
      onToggleKeepScreenOn = { viewModel.handleToggleTweak(StatusViewTweaks.KEEP_SCREEN_ON) },
      onShowProxyError = { handleOpenProxyError() },
      onShowBroadcastError = { handleOpenBroadcastError() },
      onShowPowerBalance = { viewModel.handleOpenDialog(StatusViewDialogs.POWER_BALANCE) },
      onHidePowerBalance = { viewModel.handleCloseDialog(StatusViewDialogs.POWER_BALANCE) },
      onUpdatePowerBalance = { viewModel.handleUpdatePowerBalance(it) },
      onSelectBroadcastType = { viewModel.handleUpdateBroadcastType(it) },
      onSelectPreferredNetwork = { viewModel.handleUpdatePreferredNetwork(it) },
      onHideSocketTimeout = { viewModel.handleOpenDialog(StatusViewDialogs.SOCKET_TIMEOUT) },
      onShowSocketTimeout = { viewModel.handleCloseDialog(StatusViewDialogs.SOCKET_TIMEOUT) },
      onUpdateSocketTimeout = { viewModel.handleUpdateSocketTimeout(it) },
  )
}
