/*
 * Copyright 2023 pyamsoft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import android.content.Intent
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.net.toUri
import com.pyamsoft.pydroid.arch.SaveStateDisposableEffect
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.bus.EventConsumer
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.pydroid.ui.app.LocalActivity
import com.pyamsoft.pydroid.ui.inject.ComposableInjector
import com.pyamsoft.pydroid.ui.inject.rememberComposableInjector
import com.pyamsoft.pydroid.ui.util.rememberNotNull
import com.pyamsoft.tetherfi.ObjectGraph
import com.pyamsoft.tetherfi.core.Timber
import com.pyamsoft.tetherfi.service.foreground.NotificationRefreshEvent
import com.pyamsoft.tetherfi.tile.ProxyTileService
import com.pyamsoft.tetherfi.ui.ServerViewState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class StatusInjector : ComposableInjector() {

  @JvmField @Inject internal var viewModel: StatusViewModeler? = null

  @JvmField @Inject internal var notificationRefreshBus: EventBus<NotificationRefreshEvent>? = null

  @JvmField @Inject internal var permissionRequestBus: EventBus<PermissionRequests>? = null

  @JvmField @Inject internal var permissionResponseBus: EventConsumer<PermissionResponse>? = null

  override fun onInject(activity: ComponentActivity) {
    ObjectGraph.ActivityScope.retrieve(activity).plusStatus().create().inject(this)
  }

  override fun onDispose() {
    viewModel = null
    notificationRefreshBus = null
    permissionRequestBus = null
    permissionResponseBus = null
  }
}

private fun safeOpenSettingsIntent(
    activity: ComponentActivity,
    action: String,
) {

  // Try specific first, may fail on some devices
  try {
    val intent = Intent(action, "package:${activity.packageName}".toUri())
    activity.startActivity(intent)
  } catch (e: Throwable) {
    Timber.e(e) { "Failed specific intent for $action" }
    val intent = Intent(action)
    activity.startActivity(intent)
  }
}

/** Sets up permission request interaction */
@Composable
private fun RegisterPermissionRequests(
    permissionResponseBus: Flow<PermissionResponse>,
    notificationRefreshBus: EventBus<NotificationRefreshEvent>,
    onToggleProxy: CoroutineScope.() -> Unit,
    onRefreshSystemInfo: CoroutineScope.() -> Unit,
) {
  // Create requesters
  val handleToggleProxy by rememberUpdatedState(onToggleProxy)
  val handleRefreshSystemInfo by rememberUpdatedState(onRefreshSystemInfo)

  LaunchedEffect(
      permissionResponseBus,
      notificationRefreshBus,
  ) {

    // See MainActivity
    permissionResponseBus.flowOn(context = Dispatchers.Default).also { f ->
      launch(context = Dispatchers.Default) {
        f.collect { resp ->
          when (resp) {
            is PermissionResponse.RefreshNotification -> {
              // Tell the service to refresh
              notificationRefreshBus.emit(NotificationRefreshEvent)

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
    notificationRefreshBus: EventBus<NotificationRefreshEvent>,
    onToggleProxy: CoroutineScope.() -> Unit,
    onRefreshConnection: () -> Unit
) {
  // Wrap in lambda when calling or else bad
  val handleRefreshSystemInfo by rememberUpdatedState { s: CoroutineScope ->
    viewModel.refreshSystemInfo(scope = s)
  }
  val handleRefreshConnectionInfo by rememberUpdatedState { onRefreshConnection() }

  SaveStateDisposableEffect(viewModel)

  // As early as possible because of Lifecycle quirks
  RegisterPermissionRequests(
      notificationRefreshBus = notificationRefreshBus,
      permissionResponseBus = permissionResponseBus,
      onToggleProxy = onToggleProxy,
      onRefreshSystemInfo = { handleRefreshSystemInfo(this) },
  )

  val owner = LocalLifecycleOwner.current
  LaunchedEffect(
      viewModel,
      owner,
  ) {
    viewModel.bind(
        owner = owner,
        onRefreshConnectionInfo = handleRefreshConnectionInfo,
    )
  }
}

@Composable
fun StatusEntry(
    modifier: Modifier = Modifier,
    appName: String,
    serverViewState: ServerViewState,
    onShowQRCode: () -> Unit,
    onRefreshConnection: () -> Unit,
    onJumpToHowTo: () -> Unit,
) {
  val component = rememberComposableInjector { StatusInjector() }
  val viewModel = rememberNotNull(component.viewModel)
  val permissionRequestBus = rememberNotNull(component.permissionRequestBus)
  val permissionResponseBus = rememberNotNull(component.permissionResponseBus)
  val notificationRefreshBus = rememberNotNull(component.notificationRefreshBus)

  val activity = LocalActivity.current

  val handleTileUpdate by rememberUpdatedState {
    ProxyTileService.updateTile(activity.requireNotNull())
  }

  val scope = rememberCoroutineScope()
  val handleOpenIntent by rememberUpdatedState { action: String ->
    safeOpenSettingsIntent(activity.requireNotNull(), action)
  }

  // Hooks that run on mount
  MountHooks(
      viewModel = viewModel,
      permissionResponseBus = permissionResponseBus,
      notificationRefreshBus = notificationRefreshBus,
      onToggleProxy = { viewModel.handleToggleProxy(scope = this) },
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
      onToggleProxy = { viewModel.handleToggleProxy(scope = scope) },
      onSsidChanged = { viewModel.handleSsidChanged(it.trim()) },
      onPasswordChanged = { viewModel.handlePasswordChanged(it) },
      onPortChanged = { viewModel.handlePortChanged(it) },
      onOpenBatterySettings = {
        handleOpenIntent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
      },
      onDismissBlocker = { viewModel.handleDismissBlocker(it) },
      onRequestPermissions = {
        // Request permissions
        scope.launch(context = Dispatchers.Default) {
          // See MainActivity
          permissionRequestBus.emit(PermissionRequests.Server)
        }
      },
      onOpenPermissionSettings = { handleOpenIntent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS) },
      onToggleKeepWakeLock = { viewModel.handleToggleProxyWakelock() },
      onToggleKeepWifiLock = { viewModel.handleToggleProxyWifilock() },
      onSelectBand = { viewModel.handleChangeBand(it) },
      onRequestNotificationPermission = {
        scope.launch(context = Dispatchers.Default) {
          // See MainActivity
          permissionRequestBus.emit(PermissionRequests.Notification)
        }
      },
      onStatusUpdated = { handleTileUpdate() },
      onTogglePasswordVisibility = { viewModel.handleTogglePasswordVisibility() },
      onShowNetworkError = { viewModel.handleOpenNetworkError() },
      onHideNetworkError = { viewModel.handleCloseNetworkError() },
      onShowHotspotError = { viewModel.handleOpenHotspotError() },
      onHideHotspotError = { viewModel.handleCloseHotspotError() },
      onHideSetupError = { viewModel.handleCloseSetupError() },
      onToggleIgnoreVpn = { viewModel.handleToggleIgnoreVpn() },
      onToggleShutdownWithNoClients = { viewModel.handleToggleShutdownNoClients() },
  )
}
