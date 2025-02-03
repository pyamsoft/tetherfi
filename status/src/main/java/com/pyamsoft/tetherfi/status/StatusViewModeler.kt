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

import com.pyamsoft.pydroid.arch.AbstractViewModeler
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.notify.NotifyGuard
import com.pyamsoft.tetherfi.server.ExpertPreferences
import com.pyamsoft.tetherfi.server.ProxyPreferences
import com.pyamsoft.tetherfi.server.ServerDefaults
import com.pyamsoft.tetherfi.server.ServerNetworkBand
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
      var httpPort: Boolean,
      var socksPort: Boolean,
      var band: Boolean,
  )

  private fun markPreferencesLoaded(config: LoadConfig) {
    if (config.httpPort && config.socksPort && config.ssid && config.password && config.band) {
      state.loadingState.value = StatusViewState.LoadingState.DONE
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
            httpPort = false,
            socksPort = false,
            band = false,
        )

    // Start loading
    s.loadingState.value = StatusViewState.LoadingState.LOADING

    scope.bindConfigPreferences(config)
  }

  private fun CoroutineScope.bindConfigPreferences(config: LoadConfig) {
    val scope = this
    val s = state

    // Only pull once since after this point, the state will be driven by the input
    proxyPreferences.listenForHttpPortChanges().also { f ->
      scope.launch(context = Dispatchers.Default) {
        val p = f.first()
        s.httpPort.value = if (p == 0) "" else "$p"

        config.httpPort = true
        markPreferencesLoaded(config)
      }
    }

    proxyPreferences.listenForSocksPortChanges().also { f ->
      scope.launch(context = Dispatchers.Default) {
        val p = f.first()
        s.socksPort.value = if (p == 0) "" else "$p"

        config.socksPort = true
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

  fun handleSsidChanged(ssid: String) {
    state.ssid.value = ssid
    wifiPreferences.setSsid(ssid)
  }

  fun handlePasswordChanged(password: String) {
    state.password.value = password
    wifiPreferences.setPassword(password)
  }

  fun handlePortChanged(port: String, type: ServerPortTypes) =
      when (type) {
        ServerPortTypes.HTTP -> {
          state.httpPort.value = port
          val portValue = port.toIntOrNull()
          proxyPreferences.setHttpPort(portValue ?: 0)
        }
        ServerPortTypes.SOCKS -> {
          state.socksPort.value = port
          val portValue = port.toIntOrNull()
          proxyPreferences.setSocksPort(portValue ?: 0)
        }
      }

  fun handleChangeBand(band: ServerNetworkBand) {
    state.band.value = band
    wifiPreferences.setNetworkBand(band)
  }

  fun handleTogglePasswordVisibility() {
    state.isPasswordVisible.update { !it }
  }

  fun handleUpdateBroadcastType(type: BroadcastType) {
    expertPreferences.setBroadcastType(type)
  }

  fun handleUpdatePreferredNetwork(network: PreferredNetwork) {
    expertPreferences.setPreferredNetwork(network)
  }
}
