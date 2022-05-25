package com.pyamsoft.tetherfi.status

import com.pyamsoft.pydroid.arch.AbstractViewModeler
import com.pyamsoft.tetherfi.server.ServerDefaults
import com.pyamsoft.tetherfi.server.ServerPreferences
import com.pyamsoft.tetherfi.server.battery.BatteryOptimizer
import com.pyamsoft.tetherfi.server.permission.PermissionGuard
import com.pyamsoft.tetherfi.server.status.RunningStatus
import com.pyamsoft.tetherfi.server.widi.WiDiNetwork
import com.pyamsoft.tetherfi.server.widi.receiver.WidiNetworkEvent
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class StatusViewModeler
@Inject
internal constructor(
    private val preferences: ServerPreferences,
    private val state: MutableStatusViewState,
    private val network: WiDiNetwork,
    private val permissions: PermissionGuard,
    private val batteryOptimizer: BatteryOptimizer,
) : AbstractViewModeler<StatusViewState>(state) {

  private fun toggleProxyState(
      onStart: () -> Unit,
      onStop: () -> Unit,
  ) {
    val s = state
    val requiresPermissions = !permissions.canCreateWiDiNetwork()

    s.requiresPermissions = requiresPermissions
    if (requiresPermissions) {
      s.explainPermissions = true
      return
    }

    when (val status = network.getCurrentStatus()) {
      is RunningStatus.NotRunning -> network.start(onStart)
      is RunningStatus.Running -> network.stop(onStop)
      is RunningStatus.Error -> network.stop(onStop)
      else -> {
        Timber.d("Cannot toggle while we are in the middle of an operation: $status")
      }
    }
  }

  fun loadPreferences(scope: CoroutineScope) {
    val s = state
    if (s.preferencesLoaded) {
      return
    }

    scope.launch(context = Dispatchers.Main) {
      s.port = preferences.getPort()

      if (ServerDefaults.canUseCustomConfig()) {
        s.ssid = preferences.getSsid()
        s.password = preferences.getPassword()
        s.band = preferences.getNetworkBand()
      } else {
        s.ssid = ""
        s.password = ""
        s.band = null
      }

      s.isBatteryOptimizationsIgnored = batteryOptimizer.isOptimizationsIgnored()
      s.preferencesLoaded = true
    }
  }

  fun refreshSystemInfo(scope: CoroutineScope) {
    scope.launch(context = Dispatchers.Main) {
      state.isBatteryOptimizationsIgnored = batteryOptimizer.isOptimizationsIgnored()
    }
  }

  fun refreshGroupInfo(scope: CoroutineScope) {
    scope.launch(context = Dispatchers.Main) { state.group = network.getGroupInfo() }
  }

  fun handlePermissionsExplained() {
    state.explainPermissions = false
  }

  fun watchStatusUpdates(scope: CoroutineScope) {
    scope.launch(context = Dispatchers.Main) {
      network.onProxyStatusChanged { state.proxyStatus = it }
    }

    scope.launch(context = Dispatchers.Main) { network.onStatusChanged { state.wiDiStatus = it } }

    scope.launch(context = Dispatchers.Main) {
      network.onWifiDirectEvent { event ->
        when (event) {
          is WidiNetworkEvent.ConnectionChanged -> {
            Timber.d("Connection Changed, refresh group info")
            state.ip = event.ip
            refreshGroupInfo(scope = scope)
          }
          is WidiNetworkEvent.ThisDeviceChanged -> {
            Timber.d("This Device Changed, refresh group info")
            refreshGroupInfo(scope = scope)
          }
          is WidiNetworkEvent.PeersChanged -> {
            Timber.d("Peers Changed, refresh group info")
            refreshGroupInfo(scope = scope)
          }
          is WidiNetworkEvent.WifiDisabled -> {
            Timber.d("Wifi P2P Disabled, refresh group info")
            refreshGroupInfo(scope = scope)
            network.stop { Timber.d("Proxy stopped after WiFi P2P disabled") }
          }
          is WidiNetworkEvent.WifiEnabled -> {
            Timber.d("Wifi P2P Enabled, refresh group info")
            refreshGroupInfo(scope = scope)
          }
          is WidiNetworkEvent.DiscoveryChanged -> {
            Timber.d("Discovery changed, refresh group info")
            refreshGroupInfo(scope = scope)
          }
        }
      }
    }
  }

  fun handleToggleProxy(
      scope: CoroutineScope,
      onStart: () -> Unit,
      onStop: () -> Unit,
  ) {
    toggleProxyState(
        onStart = onStart,
        onStop = onStop,
    )
    refreshGroupInfo(scope = scope)
  }

  fun handleRequestPermissions(onRequest: (List<String>) -> Unit) {
    onRequest(permissions.requiredPermissions())
  }

  fun handleSsidChanged(scope: CoroutineScope, ssid: String) {
    state.ssid = ssid
    scope.launch(context = Dispatchers.Main) { preferences.setSsid(ssid) }
  }

  fun handlePasswordChanged(scope: CoroutineScope, password: String) {
    state.password = password
    scope.launch(context = Dispatchers.Main) { preferences.setPassword(password) }
  }

  fun handlePortChanged(scope: CoroutineScope, port: String) {
    val portValue = port.toIntOrNull()
    if (portValue != null) {
      state.port = portValue
      scope.launch(context = Dispatchers.Main) { preferences.setPort(portValue) }
    }
  }
}
