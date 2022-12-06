package com.pyamsoft.tetherfi.service.tile

import android.os.Build
import android.service.quicksettings.Tile
import androidx.annotation.CheckResult
import com.pyamsoft.tetherfi.server.permission.PermissionGuard
import com.pyamsoft.tetherfi.server.status.RunningStatus
import com.pyamsoft.tetherfi.server.widi.WiDiNetworkStatus
import com.pyamsoft.tetherfi.server.widi.receiver.WiDiReceiver
import com.pyamsoft.tetherfi.server.widi.receiver.WidiNetworkEvent
import com.pyamsoft.tetherfi.service.ServiceLauncher
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber

class TileHandler
@Inject
internal constructor(
    private val receiver: WiDiReceiver,
    private val launcher: ServiceLauncher,
    private val network: WiDiNetworkStatus,
    private val permissions: PermissionGuard,

    // Don't singleton since these are per-service
    private val qsTile: () -> Tile?,
    private val showDialog: (String) -> Unit,
) {

  private var scope: CoroutineScope? = null

  /** Description message to display on the tile */
  private var tileDescription: String = ""

  @CheckResult
  private fun createOrReUseScope(): CoroutineScope {
    return scope ?: MainScope()
  }

  private fun withScope(block: suspend CoroutineScope.() -> Unit) {
    val s = createOrReUseScope()
    scope = s
    s.launch(
        context = Dispatchers.Main,
        block = block,
    )
  }

  private fun withQsTile(
      tile: Tile?,
      block: suspend (Tile) -> Unit,
  ) = withScope {
    val t = tile ?: qsTile()
    if (t == null) {
      Timber.w("QS Tile is null, cannot act")
      return@withScope
    }

    block(t)
  }

  private fun updateQsTile(
      tile: Tile?,
      block: suspend (Tile) -> Boolean,
  ) =
      withQsTile(tile) { t ->
        if (block(t)) {
          t.updateTile()
        }
      }

  private fun syncQsTile(tile: Tile?, status: RunningStatus) =
      updateQsTile(tile) { t ->
        val descChanged = t.syncDescription()
        val statusChanged = t.syncStatus(status)
        return@updateQsTile descChanged || statusChanged
      }

  @CheckResult
  private fun Tile.syncDescription(): Boolean {
    val self = this

    var changed = false

    val msg = tileDescription
    if (self.contentDescription != msg) {
      self.contentDescription = msg
      changed = true
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      if (self.stateDescription != msg) {
        self.stateDescription = msg
        changed = true
      }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      if (self.subtitle != msg) {
        self.subtitle = msg
        changed = true
      }
    }

    return changed
  }

  @CheckResult
  private fun Tile.syncStatus(status: RunningStatus): Boolean {
    val self = this

    val newState =
        when (status) {
          is RunningStatus.Error -> Tile.STATE_INACTIVE
          is RunningStatus.NotRunning -> Tile.STATE_INACTIVE
          is RunningStatus.Running -> Tile.STATE_ACTIVE
          is RunningStatus.Starting -> Tile.STATE_ACTIVE
          is RunningStatus.Stopping -> Tile.STATE_INACTIVE
        }

    return if (self.state != newState) {
      self.state = newState
      true
    } else {
      false
    }
  }

  private fun moveToErrorState(status: RunningStatus.Error, tile: Tile? = null) {
    // Display a hardcoded message here
    tileDescription = "Click to view error"

    // Upon click, the status will update again and display the right message in the Dialog
    syncQsTile(tile, status)
  }

  private fun moveToStoppedState(tile: Tile? = null) {
    tileDescription = "Not Running"
    syncQsTile(tile, RunningStatus.NotRunning)
  }

  private fun moveToStartedState(tile: Tile? = null) {
    tileDescription = "Tethering Started"
    syncQsTile(tile, RunningStatus.Running)
  }

  private fun moveToStartingState(tile: Tile? = null) {
    tileDescription = "Tethering Starting..."
    syncQsTile(tile, RunningStatus.Starting)
  }

  private fun moveToStoppingState(tile: Tile? = null) {
    tileDescription = "Tethering Stopping..."
    syncQsTile(tile, RunningStatus.Stopping)
  }

  private fun CoroutineScope.watchStatusUpdates(
      onNetworkStopped: () -> Unit,
  ) {
    val scope = this

    scope.launch(context = Dispatchers.Main) {
      launch(context = Dispatchers.Main) {
        network.onProxyStatusChanged { status ->
          when (status) {
            is RunningStatus.Error -> {
              Timber.w("Error running Proxy: ${status.message}")
              moveToErrorState(status)
            }
            is RunningStatus.NotRunning -> {
              moveToStoppedState()
            }
            is RunningStatus.Running -> {
              moveToStartedState()
            }
            is RunningStatus.Starting -> {
              moveToStartingState()
            }
            is RunningStatus.Stopping -> {
              moveToStoppingState()
            }
          }
        }
      }

      launch(context = Dispatchers.Main) {
        network.onStatusChanged { status ->
          when (status) {
            is RunningStatus.Error -> {
              Timber.w("Error running WiDi network: ${status.message}")
              moveToErrorState(status)
            }
            is RunningStatus.NotRunning -> {}
            is RunningStatus.Running -> {}
            is RunningStatus.Starting -> {}
            is RunningStatus.Stopping -> {}
          }
        }
      }

      launch(context = Dispatchers.Main) {
        receiver.onEvent { event ->
          when (event) {
            is WidiNetworkEvent.ConnectionChanged -> {}
            is WidiNetworkEvent.ThisDeviceChanged -> {}
            is WidiNetworkEvent.PeersChanged -> {}
            is WidiNetworkEvent.WifiDisabled -> {
              onNetworkStopped()
            }
            is WidiNetworkEvent.WifiEnabled -> {}
            is WidiNetworkEvent.DiscoveryChanged -> {}
          }
        }
      }
    }
  }

  private fun handleToggleProxy(
      tile: Tile?,
      onStart: () -> Unit,
      onStop: () -> Unit,
  ) {

    // Refresh these state bits
    val requiresPermissions = !permissions.canCreateWiDiNetwork()

    // If we do not have permission, stop here. s.explainPermissions will cause the permission
    // dialog
    // to show. Upon granting permission, this function will be called again and should pass
    if (requiresPermissions) {
      Timber.w("Cannot launch Proxy until Permissions are granted")
      moveToErrorState(PERMISSION_ERROR_STATE, tile)
      showDialog("Cannot start Tethering without granting Permissions.")
      // Fire on stop (which can help reset the error in the event of a WiDi error)
      onStop()
      return
    }

    val status = network.getCurrentStatus()
    syncQsTile(tile, status)
    when (status) {
      is RunningStatus.NotRunning -> onStart()
      is RunningStatus.Running -> onStop()
      is RunningStatus.Error -> {
        Timber.w("Unable to start Tethering network ${status.message}")
        moveToErrorState(status, tile)
        showDialog(status.message)

        // Fire on stop (which can help reset the error in the event of a WiDi error)
        onStop()
      }
      else -> {
        Timber.d("Cannot toggle while we are in the middle of an operation: $status")
      }
    }
  }

  fun destroy() {
    // Kill scope
    scope?.cancel()
    scope = null

    // Reset flags
    tileDescription = ""
  }

  fun bind() = withScope {
    val scope = this

    scope.watchStatusUpdates(
        onNetworkStopped = {
          Timber.d("Network stopped, stop launcher from Tile")
          launcher.stopForeground()
        },
    )
  }

  fun sync(tile: Tile?) =
      syncQsTile(
          tile,
          network.getCurrentStatus(),
      )

  fun toggleProxyNetwork(tile: Tile?) = withScope {
    handleToggleProxy(
        tile = tile,
        onStart = {
          Timber.d("Tile starts Foreground service")
          launcher.startForeground()
        },
        onStop = {
          Timber.d("Tile stops Foreground service")
          launcher.stopForeground()
        },
    )
  }

  companion object {
    private val PERMISSION_ERROR_STATE =
        RunningStatus.Error("Cannot start Tethering without granting Permissions.")
  }
}
