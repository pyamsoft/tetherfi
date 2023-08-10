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

import androidx.annotation.CheckResult
import androidx.compose.runtime.saveable.SaveableStateRegistry
import com.pyamsoft.pydroid.arch.AbstractViewModeler
import com.pyamsoft.pydroid.core.ThreadEnforcer
import com.pyamsoft.pydroid.notify.NotifyGuard
import com.pyamsoft.tetherfi.core.Timber
import com.pyamsoft.tetherfi.server.ServerDefaults
import com.pyamsoft.tetherfi.server.ServerNetworkBand
import com.pyamsoft.tetherfi.server.ServerPreferences
import com.pyamsoft.tetherfi.server.battery.BatteryOptimizer
import com.pyamsoft.tetherfi.server.permission.PermissionGuard
import com.pyamsoft.tetherfi.server.status.RunningStatus
import com.pyamsoft.tetherfi.server.widi.WiDiNetworkStatus
import com.pyamsoft.tetherfi.service.ServiceLauncher
import com.pyamsoft.tetherfi.service.ServicePreferences
import com.pyamsoft.tetherfi.status.vpn.VpnChecker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
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
    private val serviceLauncher: ServiceLauncher,
    private val vpnChecker: VpnChecker,
) : StatusViewState by state, AbstractViewModeler<StatusViewState>(state) {

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

  @CheckResult
  private fun resolveErrorFlow(): Flow<Boolean> =
      combineTransform(
              state.wiDiStatus,
              state.proxyStatus,
          ) { wifi, proxy ->
            enforcer.assertOffMainThread()

            emit(wifi is RunningStatus.Error || proxy is RunningStatus.Error)
          }
          // Distinct so that
          // Upon ON -> if any ERROR, fire dialog and in page
          // Once dialog dismissed, if OFF and error, don't dialog again because still TRUE
          // otherwise if OFF and no error, no dialog
          .distinctUntilChanged()

  @CheckResult
  private fun refreshHotspotStartBlockers(
      hasPermission: Boolean,
      isUsingVpn: Boolean,
  ): List<HotspotStartBlocker> {
    val blockers = mutableListOf<HotspotStartBlocker>()

    if (!hasPermission) {
      blockers.add(HotspotStartBlocker.PERMISSION)
    }

    if (isUsingVpn) {
      blockers.add(HotspotStartBlocker.VPN)
    }

    return blockers
  }

  private fun toggleProxy() {
    when (val status = network.getCurrentStatus()) {
      is RunningStatus.NotRunning -> {
        Timber.d { "Starting Proxy..." }
        serviceLauncher.startForeground()
      }
      is RunningStatus.Running -> {
        Timber.d { "Stopping Proxy" }
        serviceLauncher.stopForeground()
      }
      is RunningStatus.Error -> {
        Timber.d { "Resetting Proxy from Error state" }
        serviceLauncher.apply {
          resetError()
          startForeground()
        }
      }
      else -> {
        Timber.d { "Cannot toggle while we are in the middle of an operation: $status" }
      }
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
    val hasPermission = permissions.canCreateWiDiNetwork()
    s.hasHotspotPermissions.value = hasPermission
    s.isPasswordVisible.value = false

    // If something is blocking hotspot startup we will show it in the view
    val blockers =
        refreshHotspotStartBlockers(
            hasPermission = hasPermission,
            isUsingVpn = vpnChecker.isUsingVpn(),
        )
    s.startBlockers.value = blockers
    if (blockers.isNotEmpty()) {
      Timber.w { "Cannot launch Proxy until blockers are dealt with: $blockers" }
      serviceLauncher.stopForeground()
      return
    }

    toggleProxy()
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

    // Always populate the latest lock value
    servicePreferences.listenForWakeLockChanges().also { f ->
      scope.launch(context = Dispatchers.Default) {
        f.collect { keep ->
          s.keepWakeLock.value = keep

          // Watch constantly but only update the initial load config if we haven't loaded yet
          if (s.loadingState.value != StatusViewState.LoadingState.DONE) {
            config.wakelock = true
            markPreferencesLoaded(config)
          }
        }
      }
    }

    // Always populate the latest lock value
    servicePreferences.listenForWiFiLockChanges().also { f ->
      scope.launch(context = Dispatchers.Default) {
        f.collect { keep ->
          s.keepWifiLock.value = keep

          // Watch constantly but only update the initial load config if we haven't loaded yet
          if (s.loadingState.value != StatusViewState.LoadingState.DONE) {
            config.wifilock = true
            markPreferencesLoaded(config)
          }
        }
      }
    }

    // Only pull once since after this point, the state will be driven by the input
    serverPreferences.listenForPortChanges().also { f ->
      scope.launch(context = Dispatchers.Default) {
        s.port.value = f.first()

        config.port = true
        markPreferencesLoaded(config)
      }
    }

    if (ServerDefaults.canUseCustomConfig()) {
      // Only pull once since after this point, the state will be driven by the input
      serverPreferences.listenForSsidChanges().also { f ->
        scope.launch(context = Dispatchers.Default) {
          s.ssid.value = f.first()

          config.ssid = true
          markPreferencesLoaded(config)
        }
      }

      // Only pull once since after this point, the state will be driven by the input
      serverPreferences.listenForPasswordChanges().also { f ->
        scope.launch(context = Dispatchers.Default) {
          s.password.value = f.first()

          config.password = true
          markPreferencesLoaded(config)
        }
      }

      serverPreferences.listenForNetworkBandChanges().also { f ->
        scope.launch(context = Dispatchers.Default) {
          f.collect { band ->
            s.band.value = band

            // Watch constantly but only update the initial load config if we haven't loaded yet
            if (s.loadingState.value != StatusViewState.LoadingState.DONE) {
              config.band = true
              markPreferencesLoaded(config)
            }
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
    scope.launch(context = Dispatchers.Default) {
      val s = state

      // Battery optimization
      s.isBatteryOptimizationsIgnored.value = batteryOptimizer.isOptimizationsIgnored()

      // Notifications
      s.hasNotificationPermission.value = notifyGuard.canPostNotification()

      // Do we have hotspot permission
      s.hasHotspotPermissions.value = permissions.canCreateWiDiNetwork()
    }
  }

  fun handleDismissBlocker(blocker: HotspotStartBlocker) {
    state.startBlockers.update { it - blocker }
  }

  fun watchStatusUpdates(scope: CoroutineScope) {
    network.onProxyStatusChanged().also { f ->
      scope.launch(context = Dispatchers.Default) {
        f.collect { status ->
          Timber.d { "Proxy Status Changed: $status" }
          state.proxyStatus.value = status
        }
      }
    }

    network.onStatusChanged().also { f ->
      scope.launch(context = Dispatchers.Default) {
        f.collect { status ->
          Timber.d { "WiDi Status Changed: $status" }
          state.wiDiStatus.value = status
        }
      }
    }
  }

  fun bind(scope: CoroutineScope) {
    // If either of these sets an error state, we will mark the error dialog as shown
    // Need this or we run on the main thread
    resolveErrorFlow().flowOn(context = Dispatchers.Default).also { f ->
      scope.launch(context = Dispatchers.Default) {
        f.collect { show ->
          enforcer.assertOffMainThread()

          state.isShowingSetupError.value = show
        }
      }
    }
  }

  fun handleCloseSetupError() {
    state.isShowingSetupError.value = false
  }

  fun handleSsidChanged(ssid: String) {
    state.ssid.value = ssid
    serverPreferences.setSsid(ssid)
  }

  fun handlePasswordChanged(password: String) {
    state.password.value = password
    serverPreferences.setPassword(password)
  }

  fun handlePortChanged(port: String) {
    val portValue = port.toIntOrNull()
    if (portValue != null) {
      state.port.value = portValue
      serverPreferences.setPort(portValue)
    }
  }

  fun handleToggleProxyWakelock() {
    val newVal = state.keepWakeLock.updateAndGet { !it }
    servicePreferences.setWakeLock(newVal)
  }

  fun handleToggleProxyWifilock() {
    val newVal = state.keepWifiLock.updateAndGet { !it }
    servicePreferences.setWiFiLock(newVal)
  }

  fun handleChangeBand(band: ServerNetworkBand) {
    state.band.value = band
    serverPreferences.setNetworkBand(band)
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
