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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.pyamsoft.pydroid.arch.AbstractViewModeler
import com.pyamsoft.pydroid.core.ThreadEnforcer
import com.pyamsoft.pydroid.notify.NotifyGuard
import com.pyamsoft.tetherfi.core.Timber
import com.pyamsoft.tetherfi.server.ServerDefaults
import com.pyamsoft.tetherfi.server.ServerNetworkBand
import com.pyamsoft.tetherfi.server.ServerPreferences
import com.pyamsoft.tetherfi.server.battery.BatteryOptimizer
import com.pyamsoft.tetherfi.server.status.RunningStatus
import com.pyamsoft.tetherfi.server.widi.WiDiNetworkStatus
import com.pyamsoft.tetherfi.service.ServiceLauncher
import com.pyamsoft.tetherfi.service.ServicePreferences
import com.pyamsoft.tetherfi.service.prereq.HotspotRequirements
import com.pyamsoft.tetherfi.service.prereq.HotspotStartBlocker
import javax.inject.Inject
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

class StatusViewModeler
@Inject
internal constructor(
    override val state: MutableStatusViewState,
    private val enforcer: ThreadEnforcer,
    private val serverPreferences: ServerPreferences,
    private val servicePreferences: ServicePreferences,
    private val network: WiDiNetworkStatus,
    private val notifyGuard: NotifyGuard,
    private val batteryOptimizer: BatteryOptimizer,
    private val serviceLauncher: ServiceLauncher,
    private val requirements: HotspotRequirements,
) : StatusViewState by state, AbstractViewModeler<StatusViewState>(state) {

  private data class LoadConfig(
      var ssid: Boolean,
      var password: Boolean,
      var port: Boolean,
      var band: Boolean,
      var wakeLock: Boolean,
      var wifiLock: Boolean,
      var ignoreVpn: Boolean,
      var shutdownWithNoClients: Boolean,
  )

  private fun markPreferencesLoaded(config: LoadConfig) {
    if (config.port &&
        config.wifiLock &&
        config.wakeLock &&
        config.ignoreVpn &&
        config.shutdownWithNoClients &&
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

  private suspend fun startProxy() {
    val blockers = requirements.blockers()
    // If something is blocking hotspot startup we will show it in the view
    state.startBlockers.value = blockers
    if (blockers.isNotEmpty()) {
      Timber.w { "Cannot launch Proxy until blockers are dealt with: $blockers" }
      stopProxy()
      return
    }

    Timber.d { "Starting Proxy..." }
    serviceLauncher.startForeground()
  }

  private fun stopProxy() {
    Timber.d { "Stopping Proxy" }
    serviceLauncher.stopForeground()
  }

  private suspend fun resetErrorAndRestart() {
    Timber.d { "Resetting Proxy from Error state" }
    serviceLauncher.resetError()
    startProxy()
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

  fun handleToggleProxy(scope: CoroutineScope) {
    // Hide the password
    state.isPasswordVisible.value = false

    scope.launch(context = Dispatchers.Default) {
      when (val status = network.getCurrentStatus()) {
        is RunningStatus.NotRunning -> {
          startProxy()
        }
        is RunningStatus.Running -> {
          stopProxy()
        }
        is RunningStatus.Error -> {
          resetErrorAndRestart()
        }
        else -> {
          Timber.d { "Cannot toggle while we are in the middle of an operation: $status" }
        }
      }
    }
  }

  private fun loadPreferences(scope: CoroutineScope) {
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
            wakeLock = false,
            wifiLock = false,
            ignoreVpn = false,
            shutdownWithNoClients = false,
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
            config.wakeLock = true
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
            config.wifiLock = true
            markPreferencesLoaded(config)
          }
        }
      }
    }

    // Always populate the latest ignore value
    serverPreferences.listenForStartIgnoreVpn().also { f ->
      scope.launch(context = Dispatchers.Default) {
        f.collect { ignore ->
          s.isIgnoreVpn.value = ignore

          // Watch constantly but only update the initial load config if we haven't loaded yet
          if (s.loadingState.value != StatusViewState.LoadingState.DONE) {
            config.ignoreVpn = true
            markPreferencesLoaded(config)
          }
        }
      }
    }

    // Always populate the latest shutdown value
    serverPreferences.listenForShutdownWithNoClients().also { f ->
      scope.launch(context = Dispatchers.Default) {
        f.collect { shutdown ->
          s.isShutdownWithNoClients.value = shutdown

          // Watch constantly but only update the initial load config if we haven't loaded yet
          if (s.loadingState.value != StatusViewState.LoadingState.DONE) {
            config.shutdownWithNoClients = true
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
    }
  }

  private fun watchStatusUpdates(scope: CoroutineScope) {
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

  private fun watchSetupError(scope: CoroutineScope) {
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

  fun bind(
      owner: LifecycleOwner,
      onRefreshConnectionInfo: () -> Unit,
  ) {
    val scope = owner.lifecycleScope

    watchSetupError(scope)
    watchStatusUpdates(scope)
    loadPreferences(scope)

    scope.launch(context = Dispatchers.Main) {
      owner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
        // Refresh system info
        refreshSystemInfo(this)

        // Refresh connection info
        onRefreshConnectionInfo()
      }
    }
  }

  fun handleDismissBlocker(blocker: HotspotStartBlocker) {
    state.startBlockers.update { it - blocker }
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

  fun handleToggleIgnoreVpn() {
    val newVal = state.isIgnoreVpn.updateAndGet { !it }
    serverPreferences.setStartIgnoreVpn(newVal)
  }

  fun handleToggleShutdownNoClients() {
    val newVal = state.isShutdownWithNoClients.updateAndGet { !it }
    serverPreferences.setShutdownWithNoClients(newVal)
  }

  companion object {
    private const val KEY_SHOW_SETUP_ERROR = "key_show_setup_error"
    private const val KEY_SHOW_HOTSPOT_ERROR = "key_show_hotspot_error"
    private const val KEY_SHOW_NETWORK_ERROR = "key_show_network_error"
  }
}
