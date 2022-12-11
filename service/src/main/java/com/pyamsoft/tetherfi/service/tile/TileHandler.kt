package com.pyamsoft.tetherfi.service.tile

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
    private val permissionGuard: PermissionGuard,
) {

  private var scope: CoroutineScope? = null

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

  private fun CoroutineScope.watchStatusUpdates(
      onNetworkError: (RunningStatus.Error) -> Unit,
      onNetworkNotRunning: () -> Unit,
      onNetworkStarting: () -> Unit,
      onNetworkRunning: () -> Unit,
      onNetworkStopping: () -> Unit,
      onNetworkShutdown: () -> Unit,
  ) {
    val scope = this

    scope.launch(context = Dispatchers.Main) {
      launch(context = Dispatchers.Main) {
        network.onProxyStatusChanged { status ->
          when (status) {
            is RunningStatus.Error -> {
              Timber.w("Error running Proxy: ${status.message}")
              onNetworkError(status)
            }
            else -> Timber.d("Unhandled Proxy status event $status")
          }
        }
      }

      launch(context = Dispatchers.Main) {
        network.onStatusChanged { status ->
          when (status) {
            is RunningStatus.Error -> {
              Timber.w("Error running WiDi network: ${status.message}")
              onNetworkError(status)
            }
            is RunningStatus.NotRunning -> onNetworkNotRunning()
            is RunningStatus.Running -> onNetworkRunning()
            is RunningStatus.Starting -> onNetworkStarting()
            is RunningStatus.Stopping -> onNetworkStopping()
          }
        }
      }

      launch(context = Dispatchers.Main) {
        receiver.onEvent { event ->
          when (event) {
            is WidiNetworkEvent.ConnectionChanged -> {}
            is WidiNetworkEvent.ThisDeviceChanged -> {}
            is WidiNetworkEvent.PeersChanged -> {}
            is WidiNetworkEvent.WifiEnabled -> {}
            is WidiNetworkEvent.DiscoveryChanged -> {}
            is WidiNetworkEvent.WifiDisabled -> {
              onNetworkShutdown()
            }
          }
        }
      }
    }
  }

  fun destroy() {
    // Kill scope
    scope?.cancel()
    scope = null
  }

  @CheckResult
  fun getNetworkStatus(): RunningStatus {
    if (!permissionGuard.canCreateWiDiNetwork()) {
      return STATUS_MISSING_PERMISSION
    }

    return network.getCurrentStatus()
  }

  fun bind(
      onNetworkError: (RunningStatus.Error) -> Unit,
      onNetworkNotRunning: () -> Unit,
      onNetworkStarting: () -> Unit,
      onNetworkRunning: () -> Unit,
      onNetworkStopping: () -> Unit,
  ) = withScope {
    val scope = this

    scope.watchStatusUpdates(
        onNetworkError = onNetworkError,
        onNetworkNotRunning = onNetworkNotRunning,
        onNetworkStarting = onNetworkStarting,
        onNetworkRunning = onNetworkRunning,
        onNetworkStopping = onNetworkStopping,
        onNetworkShutdown = {
          Timber.d("Network stopped, stop launcher from Tile")
          launcher.stopForeground()
        },
    )
  }

  companion object {

    private val STATUS_MISSING_PERMISSION = RunningStatus.Error("Missing required permissions.")
  }
}
