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

import androidx.compose.runtime.saveable.SaveableStateRegistry
import com.pyamsoft.pydroid.arch.AbstractViewModeler
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.core.cast
import com.pyamsoft.pydroid.notify.NotifyGuard
import com.pyamsoft.tetherfi.server.ExpertPreferences
import com.pyamsoft.tetherfi.server.ProxyPreferences
import com.pyamsoft.tetherfi.server.ServerDefaults
import com.pyamsoft.tetherfi.server.ServerNetworkBand
import com.pyamsoft.tetherfi.server.ServerPerformanceLimit
import com.pyamsoft.tetherfi.server.ServerSocketTimeout
import com.pyamsoft.tetherfi.server.StatusPreferences
import com.pyamsoft.tetherfi.server.TweakPreferences
import com.pyamsoft.tetherfi.server.WifiPreferences
import com.pyamsoft.tetherfi.server.battery.BatteryOptimizer
import com.pyamsoft.tetherfi.server.broadcast.BroadcastType
import com.pyamsoft.tetherfi.server.network.PreferredNetwork
import com.pyamsoft.tetherfi.service.foreground.NotificationRefreshEvent
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch

class StatusViewModeler
@Inject
internal constructor(
    override val state: MutableStatusViewState,
    private val notificationRefreshBus: EventBus<NotificationRefreshEvent>,
    private val tweakPreferences: TweakPreferences,
    private val expertPreferences: ExpertPreferences,
    private val proxyPreferences: ProxyPreferences,
    private val statusPreferences: StatusPreferences,
    private val wifiPreferences: WifiPreferences,
    private val notifyGuard: NotifyGuard,
    private val batteryOptimizer: BatteryOptimizer,
) : StatusViewState by state, AbstractViewModeler<StatusViewState>(state) {

  private data class LoadConfig(
      var ssid: Boolean,
      var password: Boolean,
      var port: Boolean,
      var band: Boolean,
      var tweakIgnoreVpn: Boolean,
      var tweakIgnoreLocation: Boolean,
      var tweakShutdownWithNoClients: Boolean,
      var tweakKeepScreenOn: Boolean,
      var expertPowerBalance: Boolean,
      var expertSocketTimeout: Boolean,
  )

  private fun markPreferencesLoaded(config: LoadConfig) {
    if (config.port &&
        config.tweakIgnoreVpn &&
        config.tweakShutdownWithNoClients &&
        config.ssid &&
        config.password &&
        config.band &&
        config.expertPowerBalance &&
        config.expertSocketTimeout &&
        config.tweakKeepScreenOn) {
      state.loadingState.value = StatusViewState.LoadingState.DONE
    }
  }

  override fun registerSaveState(
      registry: SaveableStateRegistry
  ): List<SaveableStateRegistry.Entry> =
      mutableListOf<SaveableStateRegistry.Entry>().apply {
        registry
            .registerProvider(KEY_SHOW_POWER_BALANCE) { state.isShowingPowerBalance.value }
            .also { add(it) }

        registry
            .registerProvider(KEY_SHOW_POWER_BALANCE) { state.isShowingSocketTimeout.value }
            .also { add(it) }
      }

  override fun consumeRestoredState(registry: SaveableStateRegistry) {
    registry.consumeRestored(KEY_SHOW_POWER_BALANCE)?.cast<Boolean>()?.also {
      state.isShowingPowerBalance.value = it
    }

    registry.consumeRestored(KEY_SHOW_TIMEOUTS)?.cast<Boolean>()?.also {
      state.isShowingSocketTimeout.value = it
    }
  }

  fun handleToggleProxy(onToggleProxy: () -> Unit) {
    // Hide the password
    state.isPasswordVisible.value = false
    onToggleProxy()
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
            tweakIgnoreLocation = false,
            tweakShutdownWithNoClients = false,
            expertPowerBalance = false,
            expertSocketTimeout = false,
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
    tweakPreferences.listenForStartIgnoreVpn().also { f ->
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

    // Always populate the latest ignore value
    tweakPreferences.listenForStartIgnoreLocation().also { f ->
      scope.launch(context = Dispatchers.Default) {
        f.collect { ignore ->
          s.isIgnoreLocation.value = ignore

          // Watch constantly but only update the initial load config if we haven't loaded yet
          if (s.loadingState.value != StatusViewState.LoadingState.DONE) {
            config.tweakIgnoreLocation = true
            markPreferencesLoaded(config)
          }
        }
      }
    }

    // Always populate the latest shutdown value
    tweakPreferences.listenForShutdownWithNoClients().also { f ->
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
    expertPreferences.listenForPerformanceLimits().also { f ->
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

    // Always populate the latest socket timeout value
    expertPreferences.listenForSocketTimeout().also { f ->
      scope.launch(context = Dispatchers.Default) {
        f.collect { timeout ->
          s.socketTimeout.value = timeout

          // Watch constantly but only update the initial load config if we haven't loaded yet
          if (s.loadingState.value != StatusViewState.LoadingState.DONE) {
            config.expertSocketTimeout = true
            markPreferencesLoaded(config)
          }
        }
      }
    }

    // Only pull once since after this point, the state will be driven by the input
    proxyPreferences.listenForHttpPortChanges().also { f ->
      scope.launch(context = Dispatchers.Default) {
        val p = f.first()
        s.port.value = if (p == 0) "" else "$p"

        config.port = true
        markPreferencesLoaded(config)
      }
    }

    if (ServerDefaults.canUseCustomConfig()) {
      // Only pull once since after this point, the state will be driven by the input
      wifiPreferences.listenForSsidChanges().also { f ->
        scope.launch(context = Dispatchers.Default) {
          s.ssid.value = f.first()

          config.ssid = true
          markPreferencesLoaded(config)
        }
      }

      // Only pull once since after this point, the state will be driven by the input
      wifiPreferences.listenForPasswordChanges().also { f ->
        scope.launch(context = Dispatchers.Default) {
          s.password.value = f.first()

          config.password = true
          markPreferencesLoaded(config)
        }
      }

      wifiPreferences.listenForNetworkBandChanges().also { f ->
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

  fun bind(
      scope: CoroutineScope,
  ) {
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

  fun handleSsidChanged(ssid: String) {
    state.ssid.value = ssid
    wifiPreferences.setSsid(ssid)
  }

  fun handlePasswordChanged(password: String) {
    state.password.value = password
    wifiPreferences.setPassword(password)
  }

  fun handlePortChanged(port: String) {
    state.port.value = port

    val portValue = port.toIntOrNull()
    proxyPreferences.setHttpPort(portValue ?: 0)
  }

  fun handleChangeBand(band: ServerNetworkBand) {
    state.band.value = band
    wifiPreferences.setNetworkBand(band)
  }

  fun handleTogglePasswordVisibility() {
    state.isPasswordVisible.update { !it }
  }

  fun handleOpenDialog(dialog: StatusViewDialogs) =
      when (dialog) {
        StatusViewDialogs.POWER_BALANCE -> {
          state.isShowingPowerBalance.value = true
        }
        StatusViewDialogs.SOCKET_TIMEOUT -> {
          state.isShowingSocketTimeout.value = true
        }
      }

  fun handleCloseDialog(dialog: StatusViewDialogs) =
      when (dialog) {
        StatusViewDialogs.POWER_BALANCE -> {
          state.isShowingPowerBalance.value = false
        }
        StatusViewDialogs.SOCKET_TIMEOUT -> {
          state.isShowingSocketTimeout.value = false
        }
      }

  fun handleToggleTweak(tweak: StatusViewTweaks) =
      when (tweak) {
        StatusViewTweaks.IGNORE_VPN -> {
          val newVal = state.isIgnoreVpn.updateAndGet { !it }
          tweakPreferences.setStartIgnoreVpn(newVal)
        }
        StatusViewTweaks.IGNORE_LOCATION -> {
          val newVal = state.isIgnoreLocation.updateAndGet { !it }
          tweakPreferences.setStartIgnoreLocation(newVal)
        }
        StatusViewTweaks.KEEP_SCREEN_ON -> {
          val newVal = state.isKeepScreenOn.updateAndGet { !it }
          statusPreferences.setKeepScreenOn(newVal)
        }
        StatusViewTweaks.SHUTDOWN_NO_CLIENTS -> {
          val newVal = state.isShutdownWithNoClients.updateAndGet { !it }
          tweakPreferences.setShutdownWithNoClients(newVal)
        }
      }

  fun handleUpdatePowerBalance(limit: ServerPerformanceLimit) {
    val newVal = state.powerBalance.updateAndGet { limit }
    expertPreferences.setServerPerformanceLimit(newVal)
  }

  fun handleUpdateBroadcastType(type: BroadcastType) {
    expertPreferences.setBroadcastType(type)
  }

  fun handleUpdatePreferredNetwork(network: PreferredNetwork) {
    expertPreferences.setPreferredNetwork(network)
  }

  fun handleUpdateSocketTimeout(timeout: ServerSocketTimeout) {
    val newVal = state.socketTimeout.updateAndGet { timeout }
    expertPreferences.setSocketTimeout(newVal)
  }

  companion object {

    private const val KEY_SHOW_POWER_BALANCE = "key_show_power_balance"
    private const val KEY_SHOW_TIMEOUTS = "key_show_timeout"
  }
}
