package com.pyamsoft.tetherfi.status

import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.ui.inject.ComposableInjector
import com.pyamsoft.pydroid.ui.inject.rememberComposableInjector
import com.pyamsoft.pydroid.ui.util.LifecycleEffect
import com.pyamsoft.pydroid.ui.util.rememberActivity
import com.pyamsoft.pydroid.ui.util.rememberNotNull
import com.pyamsoft.tetherfi.ObjectGraph
import com.pyamsoft.tetherfi.service.foreground.NotificationRefreshEvent
import com.pyamsoft.tetherfi.tile.ProxyTileService
import com.pyamsoft.tetherfi.ui.ServerViewState
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

internal class StatusInjector() : ComposableInjector() {

  @JvmField @Inject internal var viewModel: StatusViewModeler? = null
  @JvmField @Inject internal var notificationRefreshBus: EventBus<NotificationRefreshEvent>? = null
  @JvmField @Inject internal var permissionRequestBus: EventBus<PermissionRequests>? = null
  @JvmField @Inject internal var permissionResponseBus: EventBus<PermissionResponse>? = null

  override fun onInject(activity: FragmentActivity) {
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
    activity: FragmentActivity,
    action: String,
) {

  // Try specific first, may fail on some devices
  try {
    val intent = Intent(action, "package:${activity.packageName}".toUri())
    activity.startActivity(intent)
  } catch (e: Throwable) {
    Timber.e(e, "Failed specific intent for $action")
    val intent = Intent(action)
    activity.startActivity(intent)
  }
}

/** Sets up permission request interaction */
@Composable
private fun RegisterPermissionRequests(
    permissionResponseBus: EventBus<PermissionResponse>,
    notificationRefreshBus: EventBus<NotificationRefreshEvent>,
    onToggleProxy: () -> Unit,
    onRefreshSystemInfo: CoroutineScope.() -> Unit,
) {
  // Create requesters
  val handleToggleProxy by rememberUpdatedState(onToggleProxy)
  val handleRefreshSystemInfo by rememberUpdatedState(onRefreshSystemInfo)

  LaunchedEffect(
      permissionResponseBus,
      notificationRefreshBus,
  ) {
    val scope = this
    scope.launch(context = Dispatchers.Main) {

      // See MainActivity
      permissionResponseBus.onEvent { resp ->
        when (resp) {
          is PermissionResponse.RefreshNotification -> {
            // Tell the service to refresh
            notificationRefreshBus.send(NotificationRefreshEvent)

            // Call to the VM to refresh info
            handleRefreshSystemInfo()
          }
          is PermissionResponse.ToggleProxy -> {
            handleToggleProxy()
          }
        }
      }
    }
  }
}

/** On mount hooks */
@Composable
private fun MountHooks(
    component: StatusInjector,
    onToggleProxy: () -> Unit,
) {
  val viewModel = rememberNotNull(component.viewModel)
  val permissionResponseBus = rememberNotNull(component.permissionResponseBus)
  val notificationRefreshBus = rememberNotNull(component.notificationRefreshBus)

  // Wrap in lambda when calling or else bad
  val handleRefreshSystemInfo by rememberUpdatedState { scope: CoroutineScope ->
    viewModel.refreshSystemInfo(scope = scope)
  }

  // As early as possible because of Lifecycle quirks
  RegisterPermissionRequests(
      notificationRefreshBus = notificationRefreshBus,
      permissionResponseBus = permissionResponseBus,
      onToggleProxy = onToggleProxy,
      onRefreshSystemInfo = { handleRefreshSystemInfo(this) },
  )

  LaunchedEffect(viewModel) {
    viewModel.loadPreferences(scope = this)
    viewModel.watchStatusUpdates(scope = this)
    handleRefreshSystemInfo(this)
  }

  LifecycleEffect {
    object : DefaultLifecycleObserver {

      override fun onResume(owner: LifecycleOwner) {
        handleRefreshSystemInfo(owner.lifecycleScope)
      }
    }
  }
}

@Composable
fun StatusEntry(
    modifier: Modifier = Modifier,
    appName: String,
    serverViewState: ServerViewState,
    onShowQRCode: () -> Unit,
    onRefreshGroup: () -> Unit,
    onRefreshConnection: () -> Unit,
) {
  val component = rememberComposableInjector { StatusInjector() }
  val viewModel = rememberNotNull(component.viewModel)
  val permissionRequestBus = rememberNotNull(component.permissionRequestBus)

  val activity = rememberActivity()
  val scope = rememberCoroutineScope()

  // Since our mount hooks use this callback in bind, we must declare it first
  val handleToggleProxy = { viewModel.handleToggleProxy() }

  // Hooks that run on mount
  MountHooks(
      component = component,
      onToggleProxy = handleToggleProxy,
  )

  val dismissPermissionPopup = { viewModel.handlePermissionsExplained() }

  val state = viewModel.state

  StatusScreen(
      modifier = modifier,
      state = state,
      serverViewState = serverViewState,
      appName = appName,
      onToggleProxy = handleToggleProxy,
      onSsidChanged = {
        viewModel.handleSsidChanged(
            scope = scope,
            ssid = it.trim(),
        )
      },
      onPasswordChanged = {
        viewModel.handlePasswordChanged(
            scope = scope,
            password = it,
        )
      },
      onPortChanged = {
        viewModel.handlePortChanged(
            scope = scope,
            port = it,
        )
      },
      onOpenBatterySettings = {
        safeOpenSettingsIntent(activity, Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
      },
      onDismissPermissionExplanation = dismissPermissionPopup,
      onRequestPermissions = {
        dismissPermissionPopup()

        // Request permissions
        scope.launch(context = Dispatchers.IO) {
          // See MainActivity
          permissionRequestBus.send(PermissionRequests.Server)
        }
      },
      onOpenPermissionSettings = {
        dismissPermissionPopup()

        safeOpenSettingsIntent(activity, Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
      },
      onToggleKeepWakeLock = {
        viewModel.handleToggleProxyWakelock(
            scope = scope,
        )
      },
      onSelectBand = {
        viewModel.handleChangeBand(
            scope = scope,
            band = it,
        )
      },
      onRequestNotificationPermission = {
        scope.launch(context = Dispatchers.IO) {
          // See MainActivity
          permissionRequestBus.send(PermissionRequests.Notification)
        }
      },
      onStatusUpdated = { ProxyTileService.updateTile(activity) },
      onTogglePasswordVisibility = { viewModel.handleTogglePasswordVisibility() },
      onShowQRCode = onShowQRCode,
      onRefreshGroup = onRefreshGroup,
      onRefreshConnection = onRefreshConnection,
  )
}
