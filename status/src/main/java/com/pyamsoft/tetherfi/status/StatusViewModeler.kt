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

import androidx.compose.runtime.saveable.SaveableStateRegistry
import com.pyamsoft.pydroid.arch.AbstractViewModeler
import com.pyamsoft.pydroid.core.ThreadEnforcer
import com.pyamsoft.pydroid.notify.NotifyGuard
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class StatusViewModeler
@Inject
internal constructor(
    override val state: MutableStatusViewState,
    private val enforcer: ThreadEnforcer,
    private val serverPreferences: ServerPreferences,
    private val servicePreferences: ServicePreferences,
    private val network: WiDiNetworkStatus,
    private val notifyGuard: NotifyGuard,
    private val permissions: PermissionGuard,
    private val batteryOptimizer: BatteryOptimizer,
    private val wiDiReceiver: WiDiReceiver,
    private val serviceLauncher: ServiceLauncher,
) : AbstractViewModeler<StatusViewState>(state) {

  private data class LoadConfig(
      var ssid: Boolean,
      var password: Boolean,
      var port: Boolean,
      var band: Boolean,
      var wakelock: Boolean,
      var wifilock: Boolean,
  )

  private fun markPreferencesLoaded(config: LoadConfig) {
    if (config.port &&
        config.wifilock &&
        config.wakelock &&
        config.ssid &&
        config.password &&
        config.band) {
      state.loadingState.value = StatusViewState.LoadingState.DONE
    }
  }

  override fun registerSaveState(
      registry: SaveableStateRegistry
  ): List<SaveableStateRegistry.Entry> =
      mutableListOf<SaveableStateRegistry.Entry>().apply {
        registry
            .registerProvider(KEY_SHOW_HOTSPOT_ERROR) { state.isShowingHotspotError.value }
            .also { add(it) }

        registry
            .registerProvider(KEY_SHOW_NETWORK_ERROR) { state.isShowingNetworkError.value }
            .also { add(it) }

        registry
            .registerProvider(KEY_SHOW_SETUP_ERROR) { state.isShowingSetupError.value }
            .also { add(it) }
      }

  override fun consumeRestoredState(registry: SaveableStateRegistry) {
    registry
        .consumeRestored(KEY_SHOW_NETWORK_ERROR)
        ?.let { it as Boolean }
        ?.also { state.isShowingNetworkError.value = it }

    registry
        .consumeRestored(KEY_SHOW_HOTSPOT_ERROR)
        ?.let { it as Boolean }
        ?.also { state.isShowingHotspotError.value = it }

    registry
        .consumeRestored(KEY_SHOW_SETUP_ERROR)
        ?.let { it as Boolean }
        ?.also { state.isShowingSetupError.value = it }
  }

  fun handleToggleProxy() {
    val s = state

    // Refresh these state bits
    val requiresPermissions = !permissions.canCreateWiDiNetwork()
    s.requiresPermissions.value = requiresPermissions
    s.explainPermissions.value = requiresPermissions
    s.isPasswordVisible.value = false

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

  fun loadPreferences(scope: CoroutineScope) {
    val s = state

    // If we are already loading, ignore this call
    if (s.loadingState.value != StatusViewState.LoadingState.NONE) {
      return
    }

    // Make this load config that we will update as things load in
    val config =
        LoadConfig(
            ssid = false,
            password = false,
            port = false,
            band = false,
            wakelock = false,
            wifilock = false,
        )

    // Start loading
    s.loadingState.value = StatusViewState.LoadingState.LOADING

    scope.launch(context = Dispatchers.Main) {
      // Always populate the latest lock value
      servicePreferences.listenForWakeLockChanges().collect { keep ->
        s.keepWakeLock.value = keep

        // Watch constantly but only update the initial load config if we haven't loaded yet
        if (s.loadingState.value != StatusViewState.LoadingState.DONE) {
          config.wakelock = true
          markPreferencesLoaded(config)
        }
      }
    }

    scope.launch(context = Dispatchers.Main) {
      // Always populate the latest lock value
      servicePreferences.listenForWiFiLockChanges().collect { keep ->
        s.keepWifiLock.value = keep

        // Watch constantly but only update the initial load config if we haven't loaded yet
        if (s.loadingState.value != StatusViewState.LoadingState.DONE) {
          config.wifilock = true
          markPreferencesLoaded(config)
        }
      }
    }

    scope.launch(context = Dispatchers.Main) {
      // Only pull once since after this point, the state will be driven by the input
      s.port.value = serverPreferences.listenForPortChanges().first()

      config.port = true
      markPreferencesLoaded(config)
    }

    if (ServerDefaults.canUseCustomConfig()) {
      scope.launch(context = Dispatchers.Main) {
        // Only pull once since after this point, the state will be driven by the input
        s.ssid.value = serverPreferences.listenForSsidChanges().first()

        config.ssid = true
        markPreferencesLoaded(config)
      }

      scope.launch(context = Dispatchers.Main) {
        // Only pull once since after this point, the state will be driven by the input
        s.password.value = serverPreferences.listenForPasswordChanges().first()

        config.password = true
        markPreferencesLoaded(config)
      }

      scope.launch(context = Dispatchers.Main) {
        serverPreferences.listenForNetworkBandChanges().collect { band ->
          s.band.value = band

          // Watch constantly but only update the initial load config if we haven't loaded yet
          if (s.loadingState.value != StatusViewState.LoadingState.DONE) {
            config.band = true
            markPreferencesLoaded(config)
          }
        }
      }
    } else {
      // No custom WiFi Direct config is allowed, fallback
      s.ssid.value = ""
      s.password.value = ""
      s.band.value = null

      // Mark loaded and attempt flag setting
      config.ssid = true
      config.password = true
      config.band = true
      markPreferencesLoaded(config)
    }
  }

  fun refreshSystemInfo(scope: CoroutineScope) {
    scope.launch(context = Dispatchers.Main) {
      val s = state

      // Battery optimization
      s.isBatteryOptimizationsIgnored.value = batteryOptimizer.isOptimizationsIgnored()

      // Notifications
      s.hasNotificationPermission.value = notifyGuard.canPostNotification()

      // If we are in an error state, we tried to run the proxy
      // If the proxy fails, we should at least check that the permission req is not the cause.
      if (s.wiDiStatus.value is RunningStatus.Error || s.proxyStatus.value is RunningStatus.Error) {
        val requiresPermissions = !permissions.canCreateWiDiNetwork()
        s.requiresPermissions.value = requiresPermissions
      }
    }
  }

  fun handlePermissionsExplained() {
    state.explainPermissions.value = false
  }

  fun watchStatusUpdates(scope: CoroutineScope) {
    scope.launch(context = Dispatchers.Main) {
      network.onProxyStatusChanged { status ->
        Timber.d("Proxy Status Changed: $status")
        state.proxyStatus.value = status
      }
    }

    scope.launch(context = Dispatchers.Main) {
      network.onStatusChanged { status ->
        Timber.d("WiDi Status Changed: $status")
        state.wiDiStatus.value = status
      }
    }

    scope.launch(context = Dispatchers.Main) {
      wiDiReceiver.onEvent { event ->
        when (event) {
          is WidiNetworkEvent.ConnectionChanged -> {}
          is WidiNetworkEvent.ThisDeviceChanged -> {}
          is WidiNetworkEvent.PeersChanged -> {}
          is WidiNetworkEvent.WifiDisabled -> {
            Timber.d("Stop ForegroundService when WiFi Disabled")
            serviceLauncher.stopForeground()
          }
          is WidiNetworkEvent.WifiEnabled -> {}
          is WidiNetworkEvent.DiscoveryChanged -> {}
        }
      }
    }
  }

  fun bind(scope: CoroutineScope) {
    scope.launch(context = Dispatchers.IO) {
      // If either of these sets an error state, we will mark the error dialog as shown
      combineTransform(
              state.wiDiStatus,
              state.proxyStatus,
          ) { wifi, proxy ->
            enforcer.assertOffMainThread()

            emit(wifi is RunningStatus.Error || proxy is RunningStatus.Error)
          }
          // Need this or we run on the main thread
          .flowOn(context = Dispatchers.IO)
          // Don't re-show an error when an error is already sent
          .distinctUntilChanged()
          .collect { show ->
            enforcer.assertOffMainThread()

            // We only care when one or both is an error and we show this additional dialog
            if (show) {
              state.isShowingSetupError.value = true
            }
          }
    }
  }

  fun handleCloseSetupError() {
    state.isShowingSetupError.value = false
  }

  fun handleSsidChanged(scope: CoroutineScope, ssid: String) {
    state.ssid.value = ssid
    scope.launch(context = Dispatchers.Main) { serverPreferences.setSsid(ssid) }
  }

  fun handlePasswordChanged(scope: CoroutineScope, password: String) {
    state.password.value = password
    scope.launch(context = Dispatchers.Main) { serverPreferences.setPassword(password) }
  }

  fun handlePortChanged(scope: CoroutineScope, port: String) {
    val portValue = port.toIntOrNull()
    if (portValue != null) {
      state.port.value = portValue
      scope.launch(context = Dispatchers.Main) { serverPreferences.setPort(portValue) }
    }
  }

  fun handleToggleProxyWakelock(scope: CoroutineScope) {
    val newVal = state.keepWakeLock.updateAndGet { !it }
    scope.launch(context = Dispatchers.Main) { servicePreferences.setWakeLock(newVal) }
  }

  fun handleToggleProxyWifilock(scope: CoroutineScope) {
    val newVal = state.keepWifiLock.updateAndGet { !it }
    scope.launch(context = Dispatchers.Main) { servicePreferences.setWiFiLock(newVal) }
  }

  fun handleChangeBand(scope: CoroutineScope, band: ServerNetworkBand) {
    state.band.value = band
    scope.launch(context = Dispatchers.Main) { serverPreferences.setNetworkBand(band) }
  }

  fun handleTogglePasswordVisibility() {
    state.isPasswordVisible.update { !it }
  }

  fun handleOpenHotspotError() {
    state.isShowingHotspotError.value = true
  }

  fun handleCloseHotspotError() {
    state.isShowingHotspotError.value = false
  }

  fun handleOpenNetworkError() {
    state.isShowingNetworkError.value = true
  }

  fun handleCloseNetworkError() {
    state.isShowingNetworkError.value = false
  }

  companion object {
    private const val KEY_SHOW_SETUP_ERROR = "key_show_setup_error"
    private const val KEY_SHOW_HOTSPOT_ERROR = "key_show_hotspot_error"
    private const val KEY_SHOW_NETWORK_ERROR = "key_show_network_error"
  }
}
