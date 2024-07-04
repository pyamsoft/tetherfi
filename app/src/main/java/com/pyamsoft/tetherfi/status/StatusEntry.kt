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
    onToggleProxy: CoroutineScope.() -> Unit,
    onRefreshSystemInfo: CoroutineScope.() -> Unit,
) {
  // Create requesters
  val handleToggleProxy by rememberUpdatedState(onToggleProxy)
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
              handleToggleProxy()
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
    onToggleProxy: CoroutineScope.() -> Unit,
    onRefreshConnection: () -> Unit
) {
  val scope = rememberCoroutineScope()

  val handleRefreshSystemInfo by rememberUpdatedState { s: CoroutineScope ->
    viewModel.handleRefreshSystemInfo(scope = s)
  }

  val handleRefreshConnectionInfo by rememberUpdatedState { onRefreshConnection() }
  val bindLifecycleResumed by rememberUpdatedState {
    viewModel.bindLifecycleResumed(
        scope = scope,
        onRefreshConnectionInfo = { handleRefreshConnectionInfo() },
    )
  }

  // As early as possible because of Lifecycle quirks
  RegisterPermissionRequests(
      permissionResponseBus = permissionResponseBus,
      onToggleProxy = onToggleProxy,
      onRefreshSystemInfo = { handleRefreshSystemInfo(this) },
  )

  SaveStateDisposableEffect(viewModel)

  LaunchedEffect(
      viewModel,
  ) {
    viewModel.bind(scope = this)
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

    // Tile
    onUpdateTile: (RunningStatus) -> Unit,
) {
  val component = rememberComposableInjector { StatusInjector() }
  val viewModel = rememberNotNull(component.viewModel)
  val permissionRequestBus = rememberNotNull(component.permissionRequestBus)
  val permissionResponseBus = rememberNotNull(component.permissionResponseBus)

  val scope = rememberCoroutineScope()

  // Hooks that run on mount
  MountHooks(
      viewModel = viewModel,
      permissionResponseBus = permissionResponseBus,
      onToggleProxy = { viewModel.handleToggleProxy() },
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
      onToggleProxy = { viewModel.handleToggleProxy() },
      onSsidChanged = { viewModel.handleSsidChanged(it.trim()) },
      onPasswordChanged = { viewModel.handlePasswordChanged(it) },
      onPortChanged = { viewModel.handlePortChanged(it) },
      onViewSlowSpeedHelp = onShowSlowSpeedHelp,
      onOpenBatterySettings = {
        onLaunchIntent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
      },
      onDismissBlocker = { viewModel.handleDismissBlocker(it) },
      onRequestPermissions = {
        // Request permissions
        scope.launch(context = Dispatchers.Default) {
          // See MainActivity
          permissionRequestBus.emit(PermissionRequests.Server)
        }
      },
      onOpenPermissionSettings = { onLaunchIntent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS) },
      onToggleKeepWakeLock = { viewModel.handleToggleProxyWakeLock() },
      onToggleKeepWifiLock = { viewModel.handleToggleProxyWifiLock() },
      onSelectBand = { viewModel.handleChangeBand(it) },
      onRequestNotificationPermission = {
        scope.launch(context = Dispatchers.Default) {
          // See MainActivity
          permissionRequestBus.emit(PermissionRequests.Notification)
        }
      },
      onStatusUpdated = onUpdateTile,
      onTogglePasswordVisibility = { viewModel.handleTogglePasswordVisibility() },
      onShowNetworkError = { viewModel.handleOpenNetworkError() },
      onHideNetworkError = { viewModel.handleCloseNetworkError() },
      onShowHotspotError = { viewModel.handleOpenHotspotError() },
      onHideHotspotError = { viewModel.handleCloseHotspotError() },
      onHideSetupError = { viewModel.handleCloseSetupError() },
      onToggleIgnoreVpn = { viewModel.handleToggleIgnoreVpn() },
      onToggleShutdownWithNoClients = { viewModel.handleToggleShutdownNoClients() },
      onToggleSocketTimeout = { viewModel.handleToggleSocketTimeout() },
      onShowProxyError = { viewModel.handleOpenProxyError() },
      onHideProxyError = { viewModel.handleCloseProxyError() },
      onShowBroadcastError = { viewModel.handleOpenBroadcastError() },
      onHideBroadcastError = { viewModel.handleCloseBroadcastError() },
      onShowPowerBalance = { viewModel.handleOpenPowerBalance() },
      onHidePowerBalance = { viewModel.handleClosePowerBalance() },
      onUpdatePowerBalance = { viewModel.handleUpdatePowerBalance(it) },
  )
}
