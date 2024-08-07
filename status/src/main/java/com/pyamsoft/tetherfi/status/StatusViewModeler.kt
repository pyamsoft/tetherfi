/*
 * Copyright 2024 pyamsoft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
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
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.core.ThreadEnforcer
import com.pyamsoft.pydroid.notify.NotifyGuard
import com.pyamsoft.tetherfi.core.AppCoroutineScope
import com.pyamsoft.tetherfi.core.Timber
import com.pyamsoft.tetherfi.server.ConfigPreferences
import com.pyamsoft.tetherfi.server.ServerDefaults
import com.pyamsoft.tetherfi.server.ServerNetworkBand
import com.pyamsoft.tetherfi.server.ServerPerformanceLimit
import com.pyamsoft.tetherfi.server.ServerPreferences
import com.pyamsoft.tetherfi.server.battery.BatteryOptimizer
import com.pyamsoft.tetherfi.server.broadcast.BroadcastNetworkStatus
import com.pyamsoft.tetherfi.server.status.RunningStatus
import com.pyamsoft.tetherfi.service.ServiceLauncher
import com.pyamsoft.tetherfi.service.foreground.NotificationRefreshEvent
import com.pyamsoft.tetherfi.service.prereq.HotspotRequirements
import com.pyamsoft.tetherfi.service.prereq.HotspotStartBlocker
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
    private val notificationRefreshBus: EventBus<NotificationRefreshEvent>,
    private val enforcer: ThreadEnforcer,
    private val configPreferences: ConfigPreferences,
    private val serverPreferences: ServerPreferences,
    private val statusPreferences: StatusPreferences,
    private val networkStatus: BroadcastNetworkStatus,
    private val notifyGuard: NotifyGuard,
    private val batteryOptimizer: BatteryOptimizer,
    private val serviceLauncher: ServiceLauncher,
    private val requirements: HotspotRequirements,
    private val appScope: AppCoroutineScope,
) : StatusViewState by state, AbstractViewModeler<StatusViewState>(state) {

  private data class LoadConfig(
      var ssid: Boolean,
      var password: Boolean,
      var port: Boolean,
      var band: Boolean,
      var tweakIgnoreVpn: Boolean,
      var tweakShutdownWithNoClients: Boolean,
      var tweakSocketTimeout: Boolean,
      var tweakKeepScreenOn: Boolean,
      var expertPowerBalance: Boolean,
  )

  private fun markPreferencesLoaded(config: LoadConfig) {
    if (config.port &&
        config.tweakIgnoreVpn &&
        config.tweakShutdownWithNoClients &&
        config.ssid &&
        config.password &&
        config.band &&
        config.expertPowerBalance &&
        config.tweakSocketTimeout &&
        config.tweakKeepScreenOn) {
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

  private fun resetError() {
    Timber.d { "Resetting Proxy from Error state" }
    serviceLauncher.resetError()
  }

  private fun closeAllDialogs() {
    handleCloseProxyError()
    handleCloseBroadcastError()
    handleCloseHotspotError()
    handleCloseSetupError()
    handleCloseNetworkError()
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

        registry
            .registerProvider(KEY_SHOW_POWER_BALANCE) { state.isShowingPowerBalance.value }
            .also { add(it) }
      }

  override fun consumeRestoredState(registry: SaveableStateRegistry) {
    registry
        .consumeRestored(KEY_SHOW_POWER_BALANCE)
        ?.let { it as Boolean }
        ?.also { state.isShowingPowerBalance.value = it }

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
    // Hide the password
    state.isPasswordVisible.value = false

    closeAllDialogs()

    appScope.launch(context = Dispatchers.Default) {
      val broadcastStatus = networkStatus.getCurrentStatus()
      val proxyStatus = networkStatus.getCurrentProxyStatus()

      if (broadcastStatus is RunningStatus.Error || proxyStatus is RunningStatus.Error) {
        // If either is in error, reset network and restart
        resetError()
      } else {
        // Otherwise just go by Wifi direct
        when (broadcastStatus) {
          is RunningStatus.NotRunning -> {
            startProxy()
          }
          is RunningStatus.Running -> {
            stopProxy()
          }
          else -> {
            Timber.d {
              "Cannot toggle while we are in the middle of an operation: $broadcastStatus"
            }
          }
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
            tweakIgnoreVpn = false,
            tweakShutdownWithNoClients = false,
            expertPowerBalance = false,
            tweakSocketTimeout = false,
            tweakKeepScreenOn = false,
        )

    // Start loading
    s.loadingState.value = StatusViewState.LoadingState.LOADING

    scope.bindConfigPreferences(config)
    scope.bindServerPreferences(config)
    scope.bindStatusPreferences(config)
  }

  private fun CoroutineScope.bindServerPreferences(config: LoadConfig) {
    val scope = this
    val s = state

    // Always populate the latest ignore value
    serverPreferences.listenForStartIgnoreVpn().also { f ->
      scope.launch(context = Dispatchers.Default) {
        f.collect { ignore ->
          s.isIgnoreVpn.value = ignore

          // Watch constantly but only update the initial load config if we haven't loaded yet
          if (s.loadingState.value != StatusViewState.LoadingState.DONE) {
            config.tweakIgnoreVpn = true
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
            config.tweakShutdownWithNoClients = true
            markPreferencesLoaded(config)
          }
        }
      }
    }

    // Always populate the latest socket timeout value
    serverPreferences.listenForTimeoutEnabled().also { f ->
      scope.launch(context = Dispatchers.Default) {
        f.collect { timeout ->
          s.isSocketTimeoutEnabled.value = timeout

          // Watch constantly but only update the initial load config if we haven't loaded yet
          if (s.loadingState.value != StatusViewState.LoadingState.DONE) {
            config.tweakSocketTimeout = true
            markPreferencesLoaded(config)
          }
        }
      }
    }
  }

  private fun CoroutineScope.bindStatusPreferences(config: LoadConfig) {
    val scope = this
    val s = state

    // Always populate the latest keep screen on value
    statusPreferences.listenForKeepScreenOn().also { f ->
      scope.launch(context = Dispatchers.Default) {
        f.collect { timeout ->
          s.isKeepScreenOn.value = timeout

          // Watch constantly but only update the initial load config if we haven't loaded yet
          if (s.loadingState.value != StatusViewState.LoadingState.DONE) {
            config.tweakKeepScreenOn = true
            markPreferencesLoaded(config)
          }
        }
      }
    }
  }

  private fun CoroutineScope.bindConfigPreferences(config: LoadConfig) {
    val scope = this
    val s = state

    // Always populate the latest power balance value
    configPreferences.listenForPerformanceLimits().also { f ->
      scope.launch(context = Dispatchers.Default) {
        f.collect { balance ->
          s.powerBalance.value = balance

          // Watch constantly but only update the initial load config if we haven't loaded yet
          if (s.loadingState.value != StatusViewState.LoadingState.DONE) {
            config.expertPowerBalance = true
            markPreferencesLoaded(config)
          }
        }
      }
    }

    // Only pull once since after this point, the state will be driven by the input
    configPreferences.listenForPortChanges().also { f ->
      scope.launch(context = Dispatchers.Default) {
        val p = f.first()
        s.port.value = if (p == 0) "" else "$p"

        config.port = true
        markPreferencesLoaded(config)
      }
    }

    if (ServerDefaults.canUseCustomConfig()) {
      // Only pull once since after this point, the state will be driven by the input
      configPreferences.listenForSsidChanges().also { f ->
        scope.launch(context = Dispatchers.Default) {
          s.ssid.value = f.first()

          config.ssid = true
          markPreferencesLoaded(config)
        }
      }

      // Only pull once since after this point, the state will be driven by the input
      configPreferences.listenForPasswordChanges().also { f ->
        scope.launch(context = Dispatchers.Default) {
          s.password.value = f.first()

          config.password = true
          markPreferencesLoaded(config)
        }
      }

      configPreferences.listenForNetworkBandChanges().also { f ->
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

  private fun watchStatusUpdates(scope: CoroutineScope) {
    networkStatus.onProxyStatusChanged().also { f ->
      scope.launch(context = Dispatchers.Default) {
        f.collect { status ->
          Timber.d { "Proxy Status Changed: $status" }
          state.proxyStatus.value = status
        }
      }
    }

    networkStatus.onStatusChanged().also { f ->
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
      scope: CoroutineScope,
  ) {
    watchSetupError(scope)
    watchStatusUpdates(scope)
    loadPreferences(scope)
  }

  fun bindLifecycleResumed(
      scope: CoroutineScope,
      onRefreshConnectionInfo: () -> Unit,
  ) {
    scope.launch(context = Dispatchers.Main) {
      // Refresh system info
      handleRefreshSystemInfo(this)

      // Refresh connection info
      onRefreshConnectionInfo()
    }
  }

  fun handleRefreshSystemInfo(scope: CoroutineScope) {
    scope.launch(context = Dispatchers.Default) {
      val s = state

      // Battery optimization
      s.isBatteryOptimizationsIgnored.value = batteryOptimizer.isOptimizationsIgnored()

      // Notifications
      s.hasNotificationPermission.value = notifyGuard.canPostNotification()

      // Tell the service to refresh
      notificationRefreshBus.emit(NotificationRefreshEvent)
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
    configPreferences.setSsid(ssid)
  }

  fun handlePasswordChanged(password: String) {
    state.password.value = password
    configPreferences.setPassword(password)
  }

  fun handlePortChanged(port: String) {
    state.port.value = port

    val portValue = port.toIntOrNull()
    configPreferences.setPort(portValue ?: 0)
  }

  fun handleChangeBand(band: ServerNetworkBand) {
    state.band.value = band
    configPreferences.setNetworkBand(band)
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

  fun handleOpenBroadcastError() {
    state.isShowingBroadcastError.value = true
  }

  fun handleCloseBroadcastError() {
    state.isShowingBroadcastError.value = false
  }

  fun handleOpenProxyError() {
    state.isShowingProxyError.value = true
  }

  fun handleCloseProxyError() {
    state.isShowingProxyError.value = false
  }

  fun handleOpenPowerBalance() {
    state.isShowingPowerBalance.value = true
  }

  fun handleClosePowerBalance() {
    state.isShowingPowerBalance.value = false
  }

  fun handleToggleIgnoreVpn() {
    val newVal = state.isIgnoreVpn.updateAndGet { !it }
    serverPreferences.setStartIgnoreVpn(newVal)
  }

  fun handleToggleShutdownNoClients() {
    val newVal = state.isShutdownWithNoClients.updateAndGet { !it }
    serverPreferences.setShutdownWithNoClients(newVal)
  }

  fun handleUpdatePowerBalance(limit: ServerPerformanceLimit) {
    val newVal = state.powerBalance.updateAndGet { limit }
    configPreferences.setServerPerformanceLimit(newVal)
  }

  fun handleToggleSocketTimeout() {
    val newVal = state.isSocketTimeoutEnabled.updateAndGet { !it }
    serverPreferences.setTimeoutEnabled(newVal)
  }

  fun handleToggleKeepScreenOn() {
    val newVal = state.isKeepScreenOn.updateAndGet { !it }
    statusPreferences.setKeepScreenOn(newVal)
  }

  companion object {
    private const val KEY_SHOW_SETUP_ERROR = "key_show_setup_error"
    private const val KEY_SHOW_HOTSPOT_ERROR = "key_show_hotspot_error"
    private const val KEY_SHOW_NETWORK_ERROR = "key_show_network_error"

    private const val KEY_SHOW_POWER_BALANCE = "key_show_power_balance"
  }
}
