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

package com.pyamsoft.tetherfi.main

import androidx.compose.runtime.saveable.SaveableStateRegistry
import com.pyamsoft.pydroid.arch.AbstractViewModeler
import com.pyamsoft.tetherfi.core.InAppRatingPreferences
import com.pyamsoft.tetherfi.server.ServerPreferences
import com.pyamsoft.tetherfi.server.status.RunningStatus
import com.pyamsoft.tetherfi.server.widi.WiDiNetworkStatus
import com.pyamsoft.tetherfi.server.widi.receiver.WiDiReceiver
import com.pyamsoft.tetherfi.server.widi.receiver.WidiNetworkEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

class MainViewModeler
@Inject
internal constructor(
    override val state: MutableMainViewState,
    private val network: WiDiNetworkStatus,
    private val serverPreferences: ServerPreferences,
    private val wiDiReceiver: WiDiReceiver,
    private val inAppRatingPreferences: InAppRatingPreferences,
) : MainViewState by state, AbstractViewModeler<MainViewState>(state) {

  private val isNetworkCurrentlyRunning =
      MutableStateFlow(network.getCurrentStatus() == RunningStatus.Running)

  fun watchForInAppRatingPrompt(
      scope: CoroutineScope,
      onShowInAppRating: () -> Unit,
  ) {
    inAppRatingPreferences
        .listenShowInAppRating()
        .filter { it }
        .distinctUntilChanged()
        .also { f ->
          scope.launch(context = Dispatchers.Default) {
            f.collect { show ->
              if (show) {
                Timber.d("Show in-app rating")
                withContext(context = Dispatchers.Main) { onShowInAppRating() }
              }
            }
          }
        }
  }

  fun bind(scope: CoroutineScope) {
    val s = state

    // Watch group info
    network.onGroupInfoChanged().also { f ->
      scope.launch(context = Dispatchers.Default) { f.collect { s.group.value = it } }
    }

    // Watch connection info
    network.onConnectionInfoChanged().also { f ->
      scope.launch(context = Dispatchers.Default) { f.collect { s.connection.value = it } }
    }

    // Watch the server status and update if it is running
    network.onStatusChanged().also { f ->
      scope.launch(context = Dispatchers.Default) {
        f.collect { s ->
          val wasRunning = isNetworkCurrentlyRunning.value
          val currentlyRunning = s == RunningStatus.Running
          isNetworkCurrentlyRunning.value = currentlyRunning

          // If the network was switched off, clear everything
          if (wasRunning && !currentlyRunning) {
            Timber.d("Hotspot was turned OFF, refresh network settings to clear")

            // Refresh connection info, should blank out
            handleRefreshConnectionInfo(this)

            // Explicitly close the QR code
            handleCloseQRCodeDialog()
          } else if (!wasRunning && currentlyRunning) {
            Timber.d("Hotspot was turned ON, refresh network settings to update")

            // Refresh connection info, should populate
            handleRefreshConnectionInfo(this)
          }
        }
      }
    }

    // Port is its own thing, not part of group info
    serverPreferences.listenForPortChanges().also { f ->
      scope.launch(context = Dispatchers.Default) { f.collect { s.port.value = it } }
    }

    // But then once we are done editing and we start getting events from the receiver,
    // take them instead
    wiDiReceiver.listenNetworkEvents().also { f ->
      scope.launch(context = Dispatchers.Default) {
        f.collect { event ->
          when (event) {
            is WidiNetworkEvent.ConnectionChanged -> {
              s.connection.update { info -> info.update { it.copy(hostName = event.hostName) } }
              handleRefreshConnectionInfo(this)
            }
            is WidiNetworkEvent.ThisDeviceChanged -> {
              handleRefreshConnectionInfo(this)
            }
            is WidiNetworkEvent.PeersChanged -> {
              handleRefreshConnectionInfo(this)
            }
            is WidiNetworkEvent.WifiDisabled -> {
              handleRefreshConnectionInfo(this)
            }
            is WidiNetworkEvent.WifiEnabled -> {
              handleRefreshConnectionInfo(this)
            }
            is WidiNetworkEvent.DiscoveryChanged -> {
              handleRefreshConnectionInfo(this)
            }
          }
        }
      }
    }
  }

  override fun registerSaveState(
      registry: SaveableStateRegistry
  ): List<SaveableStateRegistry.Entry> =
      mutableListOf<SaveableStateRegistry.Entry>().apply {
        val s = state

        registry.registerProvider(KEY_IS_SETTINGS_OPEN) { s.isSettingsOpen.value }.also { add(it) }
        registry
            .registerProvider(KEY_IS_SHOWING_QR) { s.isShowingQRCodeDialog.value }
            .also { add(it) }
      }

  override fun consumeRestoredState(registry: SaveableStateRegistry) {
    val s = state
    registry
        .consumeRestored(KEY_IS_SETTINGS_OPEN)
        ?.let { it as Boolean }
        ?.also { s.isSettingsOpen.value = it }

    registry
        .consumeRestored(KEY_IS_SHOWING_QR)
        ?.let { it as Boolean }
        ?.also { s.isShowingQRCodeDialog.value = it }
  }

  fun handleRefreshConnectionInfo(scope: CoroutineScope) {
    scope.launch(context = Dispatchers.Default) { network.updateNetworkInfo() }
  }

  fun handleOpenSettings() {
    state.isSettingsOpen.value = true
  }

  fun handleCloseSettings() {
    state.isSettingsOpen.value = false
  }

  fun handleOpenQRCodeDialog() {
    // If the hotspot is valid, we will have this from the group
    val isHotspotDataValid = state.group.value is WiDiNetworkStatus.GroupInfo.Connected
    state.isShowingQRCodeDialog.value = isHotspotDataValid && isNetworkCurrentlyRunning.value
  }

  fun handleCloseQRCodeDialog() {
    state.isShowingQRCodeDialog.value = false
  }

  fun handleAnalyticsMarkOpened() {
    inAppRatingPreferences.markAppOpened()
  }

  companion object {

    private const val KEY_IS_SETTINGS_OPEN = "is_settings_open"
    private const val KEY_IS_SHOWING_QR = "show_qr"
  }
}
