package com.pyamsoft.tetherfi.status

import android.content.Intent
import android.provider.Settings
import androidx.annotation.CheckResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.notify.NotifyGuard
import com.pyamsoft.pydroid.ui.inject.ComposableInjector
import com.pyamsoft.pydroid.ui.inject.rememberComposableInjector
import com.pyamsoft.pydroid.ui.util.LifecycleEffect
import com.pyamsoft.pydroid.ui.util.rememberActivity
import com.pyamsoft.pydroid.ui.util.rememberNotNull
import com.pyamsoft.pydroid.util.PermissionRequester
import com.pyamsoft.pydroid.util.doOnResume
import com.pyamsoft.tetherfi.ObjectGraph
import com.pyamsoft.tetherfi.server.ServerNetworkBand
import com.pyamsoft.tetherfi.server.status.RunningStatus
import com.pyamsoft.tetherfi.service.foreground.NotificationRefreshEvent
import com.pyamsoft.tetherfi.tile.ProxyTileService
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

internal class StatusInjector : ComposableInjector() {

  @JvmField @Inject internal var viewModel: StatusViewModeler? = null
  @JvmField @Inject internal var notifyGuard: NotifyGuard? = null
  @JvmField @Inject internal var notificationRefreshBus: EventBus<NotificationRefreshEvent>? = null
  @JvmField @Inject internal var permissionRequestBus: EventBus<PermissionRequests>? = null

  override fun onInject(activity: FragmentActivity) {
    ObjectGraph.ActivityScope.retrieve(activity).plusStatus().create().inject(this)
  }

  override fun onDispose() {
    viewModel = null
    notifyGuard = null
    notificationRefreshBus = null
    permissionRequestBus = null
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

private data class MountHookResults(
    val notificationState: State<Boolean>,
)

/** Sets up permission request interaction */
@Composable
private fun RegisterPermissionRequests(
    notificationPermissionState: MutableState<Boolean>,
    permissionRequestBus: EventBus<PermissionRequests>,
    notificationRefreshBus: EventBus<NotificationRefreshEvent>,
    onToggleProxy: () -> Unit,
) {
  // Create requesters
  val handleToggleProxy = rememberUpdatedState(onToggleProxy)

  LaunchedEffect(
      permissionRequestBus,
      handleToggleProxy,
      notificationRefreshBus,
      notificationPermissionState,
  ) {
    val scope = this
    scope.launch(context = Dispatchers.Main) {
      permissionRequestBus.onEvent { request ->
        when (request) {
          is PermissionRequests.RefreshNotificationPermission -> {
            // Update state variable
            notificationPermissionState.value = true
            notificationRefreshBus.send(NotificationRefreshEvent)
          }
          is PermissionRequests.ToggleProxy -> {
            handleToggleProxy.value.invoke()
          }
        }
      }
    }
  }
}

/** On mount hooks */
@Composable
@CheckResult
private fun mountHooks(
    component: StatusInjector,
    onToggleProxy: () -> Unit,
): MountHookResults {
  val viewModel = rememberNotNull(component.viewModel)
  val notifyGuard = rememberNotNull(component.notifyGuard)
  val permissionRequestBus = rememberNotNull(component.permissionRequestBus)
  val notificationRefreshBus = rememberNotNull(component.notificationRefreshBus)

  val notificationState = remember { mutableStateOf(notifyGuard.canPostNotification()) }

  // As early as possible because of Lifecycle quirks
  RegisterPermissionRequests(
      notificationPermissionState = notificationState,
      notificationRefreshBus = notificationRefreshBus,
      permissionRequestBus = permissionRequestBus,
      onToggleProxy = onToggleProxy,
  )

  val owner = LocalLifecycleOwner.current
  val activity = rememberActivity()

  LaunchedEffect(
      viewModel,
      owner,
  ) {
    viewModel.refreshGroupInfo(scope = owner.lifecycleScope)
  }

  LaunchedEffect(viewModel, owner) { viewModel.watchStatusUpdates(scope = owner.lifecycleScope) }

  LaunchedEffect(viewModel, owner, activity) {
    viewModel.loadPreferences(scope = owner.lifecycleScope) {
      // Vitals after prefs are loaded
      owner.doOnResume { activity.reportFullyDrawn() }
    }
  }

  LifecycleEffect {
    object : DefaultLifecycleObserver {

      override fun onResume(owner: LifecycleOwner) {
        viewModel.refreshSystemInfo(scope = owner.lifecycleScope)
      }
    }
  }

  return remember(
      notificationState,
  ) {
    MountHookResults(
        notificationState = notificationState,
    )
  }
}

@Composable
fun StatusEntry(
    modifier: Modifier = Modifier,
    appName: String,
    notificationRequester: PermissionRequester.Requester,
    serverRequester: PermissionRequester.Requester,
) {
  val component = rememberComposableInjector { StatusInjector() }
  val viewModel = rememberNotNull(component.viewModel)

  val activity = rememberActivity()
  val owner = LocalLifecycleOwner.current

  // Since our mount hooks use this callback in bind, we must declare it first
  val handleToggleProxy by rememberUpdatedState {
    viewModel.handleToggleProxy(scope = owner.lifecycleScope)
  }

  // Hooks that run on mount
  val hooks =
      mountHooks(
          component = component,
          onToggleProxy = handleToggleProxy,
      )

  //
  val notificationState = hooks.notificationState

  val handleStatusUpdated by rememberUpdatedState { _: RunningStatus ->
    ProxyTileService.updateTile(activity)
  }

  val handleRequestNotificationPermission by rememberUpdatedState {
    notificationRequester.requestPermissions()
  }

  val handleSsidChanged by rememberUpdatedState { ssid: String ->
    viewModel.handleSsidChanged(
        scope = owner.lifecycleScope,
        ssid = ssid.trim(),
    )
  }

  val handlePasswordChanged by rememberUpdatedState { password: String ->
    viewModel.handlePasswordChanged(
        scope = owner.lifecycleScope,
        password = password,
    )
  }

  val handlePortChanged by rememberUpdatedState { port: String ->
    viewModel.handlePortChanged(
        scope = owner.lifecycleScope,
        port = port,
    )
  }

  val handleOpenBatterySettings by rememberUpdatedState {
    safeOpenSettingsIntent(activity, Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
  }

  val handleToggleProxyWakelock by rememberUpdatedState {
    viewModel.handleToggleProxyWakelock(
        scope = owner.lifecycleScope,
    )
  }

  val handleChangeBand by rememberUpdatedState { band: ServerNetworkBand ->
    viewModel.handleChangeBand(
        scope = owner.lifecycleScope,
        band = band,
    )
  }

  val handleRequestServerPermissions by rememberUpdatedState {
    // Close dialog
    viewModel.handlePermissionsExplained()

    // Request permissions
    serverRequester.requestPermissions()
  }

  val handleOpenApplicationSettings by rememberUpdatedState {
    viewModel.handlePermissionsExplained()

    // Open settings
    safeOpenSettingsIntent(activity, Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
  }

  val handleDismissPermissionExplanation by rememberUpdatedState {
    viewModel.handlePermissionsExplained()
  }

  StatusScreen(
      modifier = modifier,
      state = viewModel.state(),
      appName = appName,
      hasNotificationPermission = notificationState.value,
      onToggle = handleToggleProxy,
      onSsidChanged = handleSsidChanged,
      onPasswordChanged = handlePasswordChanged,
      onPortChanged = handlePortChanged,
      onOpenBatterySettings = handleOpenBatterySettings,
      onDismissPermissionExplanation = handleDismissPermissionExplanation,
      onRequestPermissions = handleRequestServerPermissions,
      onOpenPermissionSettings = handleOpenApplicationSettings,
      onToggleKeepWakeLock = handleToggleProxyWakelock,
      onSelectBand = handleChangeBand,
      onRequestNotificationPermission = handleRequestNotificationPermission,
      onStatusUpdated = handleStatusUpdated,
  )
}
