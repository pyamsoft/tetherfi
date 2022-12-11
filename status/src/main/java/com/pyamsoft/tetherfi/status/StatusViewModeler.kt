package com.pyamsoft.tetherfi.status

import com.pyamsoft.pydroid.arch.AbstractViewModeler
import com.pyamsoft.tetherfi.server.ServerDefaults
import com.pyamsoft.tetherfi.server.ServerNetworkBand
import com.pyamsoft.tetherfi.server.ServerPreferences
import com.pyamsoft.tetherfi.server.battery.BatteryOptimizer
import com.pyamsoft.tetherfi.server.permission.PermissionGuard
import com.pyamsoft.tetherfi.server.status.RunningStatus
import com.pyamsoft.tetherfi.server.widi.WiDiNetworkStatus
import com.pyamsoft.tetherfi.server.widi.receiver.WiDiReceiver
import com.pyamsoft.tetherfi.server.widi.receiver.WidiNetworkEvent
import com.pyamsoft.tetherfi.service.ServiceLauncher
import com.pyamsoft.tetherfi.service.ServicePreferences
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

class StatusViewModeler
@Inject
internal constructor(
    private val state: MutableStatusViewState,
    private val serverPreferences: ServerPreferences,
    private val servicePreferences: ServicePreferences,
    private val network: WiDiNetworkStatus,
    private val permissions: PermissionGuard,
    private val batteryOptimizer: BatteryOptimizer,
    private val wiDiReceiver: WiDiReceiver,
    private val serviceLauncher: ServiceLauncher,
) : AbstractViewModeler<StatusViewState>(state) {

  private data class LoadConfig(
      var port: Boolean,
      var wakelock: Boolean,
      var ssid: Boolean,
      var password: Boolean,
      var band: Boolean,
  )

  private fun markPreferencesLoaded(config: LoadConfig) {
    if (config.port && config.wakelock && config.ssid && config.password && config.band) {
      state.preferencesLoaded = true
    }
  }

  private fun toggleProxy() {
    val s = state

    // Collapse instructions by default
    s.isConnectionInstructionExpanded = false

    // Refresh these state bits
    val requiresPermissions = !permissions.canCreateWiDiNetwork()
    s.requiresPermissions = requiresPermissions
    s.explainPermissions = requiresPermissions

    // If we do not have permission, stop here. s.explainPermissions will cause the permission
    // dialog
    // to show. Upon granting permission, this function will be called again and should pass
    if (requiresPermissions) {
      Timber.w("Cannot launch Proxy until Permissions are granted")
      serviceLauncher.stopForeground()
      return
    }

    when (val status = network.getCurrentStatus()) {
      is RunningStatus.NotRunning -> {
        Timber.d("Starting Proxy...")
        serviceLauncher.startForeground()
      }
      is RunningStatus.Running -> {
        Timber.d("Stopping Proxy")
        serviceLauncher.stopForeground()
      }
      is RunningStatus.Error -> {
        Timber.w("Resetting Proxy from Error state")
        serviceLauncher.stopForeground()
      }
      else -> {
        Timber.d("Cannot toggle while we are in the middle of an operation: $status")
      }
    }
  }

  fun loadPreferences(
      scope: CoroutineScope,
      andThen: () -> Unit,
  ) {
    val s = state
    if (s.preferencesLoaded) {
      andThen()
      return
    }

    val config =
        LoadConfig(
            port = false,
            wakelock = false,
            ssid = false,
            password = false,
            band = false,
        )

    scope.launch(context = Dispatchers.Main) {
      // Always populate the latest port value
      servicePreferences.listenForWakeLockChanges().collectLatest { keep ->
        s.keepWakeLock = keep

        // Watch constantly but only update the initial load config if we haven't loaded yet
        if (!s.preferencesLoaded) {
          config.wakelock = true
          markPreferencesLoaded(config)
        }
      }
    }

    scope.launch(context = Dispatchers.Main) {
      // Only pull once since after this point, the state will be driven by the input
      s.port = serverPreferences.listenForPortChanges().first()

      config.port = true
      markPreferencesLoaded(config)
    }

    if (ServerDefaults.canUseCustomConfig()) {
      scope.launch(context = Dispatchers.Main) {
        // Only pull once since after this point, the state will be driven by the input
        s.ssid = serverPreferences.listenForSsidChanges().first()

        config.ssid = true
        markPreferencesLoaded(config)
      }

      scope.launch(context = Dispatchers.Main) {
        // Only pull once since after this point, the state will be driven by the input
        s.password = serverPreferences.listenForPasswordChanges().first()

        config.password = true
        markPreferencesLoaded(config)
      }

      scope.launch(context = Dispatchers.Main) {
        serverPreferences.listenForNetworkBandChanges().collectLatest { band ->
          s.band = band

          // Watch constantly but only update the initial load config if we haven't loaded yet
          if (!s.preferencesLoaded) {
            config.band = true
            markPreferencesLoaded(config)
          }
        }
      }
    } else {
      // No custom WiFi Direct config is allowed, fallback
      s.ssid = ""
      s.password = ""
      s.band = null

      // Mark loaded and attempt flag setting
      config.ssid = true
      config.password = true
      config.band = true
      markPreferencesLoaded(config)
    }

    andThen()
  }

  fun refreshSystemInfo(scope: CoroutineScope) {
    scope.launch(context = Dispatchers.Main) {
      val s = state

      // Battery optimization
      s.isBatteryOptimizationsIgnored = batteryOptimizer.isOptimizationsIgnored()

      // If we are in an error state, we tried to run the proxy
      // If the proxy fails, we should at least check that the permission req is not the cause.
      if (s.wiDiStatus is RunningStatus.Error || s.proxyStatus is RunningStatus.Error) {
        val requiresPermissions = !permissions.canCreateWiDiNetwork()
        s.requiresPermissions = requiresPermissions
      }
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
      network.onProxyStatusChanged { status ->
        Timber.d("Proxy Status Changed: $status")
        state.proxyStatus = status
      }
    }

    scope.launch(context = Dispatchers.Main) {
      network.onStatusChanged { status ->
        Timber.d("WiDi Status Changed: $status")
        state.wiDiStatus = status
      }
    }

    scope.launch(context = Dispatchers.Main) {
      wiDiReceiver.onEvent { event ->
        when (event) {
          is WidiNetworkEvent.ConnectionChanged -> {
            state.ip = event.ip
            refreshGroupInfo(scope = scope)
          }
          is WidiNetworkEvent.ThisDeviceChanged -> {
            refreshGroupInfo(scope = scope)
          }
          is WidiNetworkEvent.PeersChanged -> {
            refreshGroupInfo(scope = scope)
          }
          is WidiNetworkEvent.WifiDisabled -> {
            refreshGroupInfo(scope = scope)

            Timber.d("Stop ForegroundService when WiFi Disabled")
            serviceLauncher.stopForeground()
          }
          is WidiNetworkEvent.WifiEnabled -> {
            refreshGroupInfo(scope = scope)
          }
          is WidiNetworkEvent.DiscoveryChanged -> {
            refreshGroupInfo(scope = scope)
          }
        }
      }
    }
  }

  fun handleToggleProxy(
      scope: CoroutineScope,
  ) {
    toggleProxy()
    refreshGroupInfo(scope = scope)
  }

  fun handleSsidChanged(scope: CoroutineScope, ssid: String) {
    state.ssid = ssid
    scope.launch(context = Dispatchers.Main) { serverPreferences.setSsid(ssid) }
  }

  fun handlePasswordChanged(scope: CoroutineScope, password: String) {
    state.password = password
    scope.launch(context = Dispatchers.Main) { serverPreferences.setPassword(password) }
  }

  fun handlePortChanged(scope: CoroutineScope, port: String) {
    val portValue = port.toIntOrNull()
    if (portValue != null) {
      state.port = portValue
      scope.launch(context = Dispatchers.Main) { serverPreferences.setPort(portValue) }
    }
  }

  fun handleToggleConnectionInstructions() {
    val s = state
    s.isConnectionInstructionExpanded = !s.isConnectionInstructionExpanded
  }

  fun handleToggleProxyWakelock(scope: CoroutineScope) {
    val s = state
    s.keepWakeLock = !s.keepWakeLock
    scope.launch(context = Dispatchers.Main) { servicePreferences.setWakeLock(s.keepWakeLock) }
  }

  fun handleChangeBand(scope: CoroutineScope, band: ServerNetworkBand) {
    state.band = band
    scope.launch(context = Dispatchers.Main) { serverPreferences.setNetworkBand(band) }
  }
}
