package com.pyamsoft.tetherfi.tile

import com.pyamsoft.pydroid.arch.AbstractViewModeler
import com.pyamsoft.tetherfi.server.permission.PermissionGuard
import com.pyamsoft.tetherfi.server.status.RunningStatus
import com.pyamsoft.tetherfi.service.ServiceLauncher
import com.pyamsoft.tetherfi.service.tile.TileHandler
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class ProxyTileViewModeler
@Inject
internal constructor(
    override val state: MutableProxyTileViewState,
    private val permissions: PermissionGuard,
    private val handler: TileHandler,
    private val serviceLauncher: ServiceLauncher,
) : AbstractViewModeler<ProxyTileViewState>(state) {

  init {
    // Sync up the network state on init so that we can immediately capture it in the View
    state.status.value = handler.getNetworkStatus()
  }

  fun handleDismissed() {
    state.isShowing.value = false
  }

  fun bind(scope: CoroutineScope) {
    val s = state

    scope.launch(context = Dispatchers.Main) {
      handler.bind(
          onNetworkError = { err -> s.status.value = err },
          onNetworkStarting = { s.status.value = RunningStatus.Starting },
          onNetworkStopping = { s.status.value = RunningStatus.Stopping },
          onNetworkNotRunning = { s.status.value = RunningStatus.NotRunning },
          onNetworkRunning = { s.status.value = RunningStatus.Running },
      )
    }
  }

  fun handleToggleProxy() {
    val s = state

    // Refresh these state bits
    val requiresPermissions = !permissions.canCreateWiDiNetwork()

    // If we do not have permission, stop here. s.explainPermissions will cause the permission
    // dialog
    // to show. Upon granting permission, this function will be called again and should pass
    if (requiresPermissions) {
      Timber.w("Cannot launch Proxy until Permissions are granted")
      s.status.value = RunningStatus.Error("Missing required permission, cannot start Hotspot")
      serviceLauncher.stopForeground()
      return
    }

    when (val status = handler.getNetworkStatus()) {
      is RunningStatus.NotRunning -> {
        Timber.d("Starting Proxy...")
        serviceLauncher.startForeground()
      }
      is RunningStatus.Running -> {
        Timber.d("Stopping Proxy")
        serviceLauncher.stopForeground()
      }
      else -> {
        Timber.d("Cannot toggle while we are in the middle of an operation: $status")
      }
    }
  }
}
