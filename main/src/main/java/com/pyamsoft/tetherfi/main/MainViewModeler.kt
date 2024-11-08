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

package com.pyamsoft.tetherfi.main

import androidx.compose.runtime.saveable.SaveableStateRegistry
import com.pyamsoft.pydroid.arch.AbstractViewModeler
import com.pyamsoft.pydroid.core.cast
import com.pyamsoft.tetherfi.core.InAppRatingPreferences
import com.pyamsoft.tetherfi.core.Timber
import com.pyamsoft.tetherfi.server.ExpertPreferences
import com.pyamsoft.tetherfi.server.ProxyPreferences
import com.pyamsoft.tetherfi.server.broadcast.BroadcastEvent
import com.pyamsoft.tetherfi.server.broadcast.BroadcastNetworkStatus
import com.pyamsoft.tetherfi.server.broadcast.BroadcastNetworkUpdater
import com.pyamsoft.tetherfi.server.broadcast.BroadcastObserver
import com.pyamsoft.tetherfi.server.status.RunningStatus
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModeler
@Inject
internal constructor(
    override val state: MutableMainViewState,
    private val networkStatus: BroadcastNetworkStatus,
    private val networkUpdater: BroadcastNetworkUpdater,
    private val broadcastObserver: BroadcastObserver,
    private val inAppRatingPreferences: InAppRatingPreferences,
    private val proxyPreferences: ProxyPreferences,
    private val expertPreferences: ExpertPreferences,
) : MainViewState by state, AbstractViewModeler<MainViewState>(state) {

  private val isNetworkCurrentlyRunning =
      MutableStateFlow(networkStatus.getCurrentStatus() == RunningStatus.Running)

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
                Timber.d { "Show in-app rating" }
                withContext(context = Dispatchers.Main) { onShowInAppRating() }
              }
            }
          }
        }
  }

  fun bind(scope: CoroutineScope) {
    val s = state

    // Watch group info
    networkStatus.onGroupInfoChanged().also { f ->
      scope.launch(context = Dispatchers.Default) { f.collect { s.group.value = it } }
    }

    // Watch connection info
    networkStatus.onConnectionInfoChanged().also { f ->
      scope.launch(context = Dispatchers.Default) { f.collect { s.connection.value = it } }
    }

    // Watch the server status and update if it is running
    networkStatus.onStatusChanged().also { f ->
      scope.launch(context = Dispatchers.Default) {
        f.collect { s ->
          val wasRunning = isNetworkCurrentlyRunning.value
          val currentlyRunning = s == RunningStatus.Running
          isNetworkCurrentlyRunning.value = currentlyRunning

          // If the network was switched off, clear everything
          if (wasRunning && !currentlyRunning) {
            Timber.d { "Hotspot was turned OFF, refresh network settings to clear" }

            // Refresh connection info, should blank out
            handleRefreshConnectionInfo(this)

            // Explicitly close the QR code
            handleCloseQRCodeDialog()
          } else if (!wasRunning && currentlyRunning) {
            Timber.d { "Hotspot was turned ON, refresh network settings to update" }

            // Refresh connection info, should populate
            handleRefreshConnectionInfo(this)
          }
        }
      }
    }

    // Port is its own thing, not part of group info
    proxyPreferences.listenForPortChanges().also { f ->
      scope.launch(context = Dispatchers.Default) { f.collect { s.port.value = it } }
    }

    // But then once we are done editing and we start getting events from the receiver,
    // take them instead
    broadcastObserver.listenNetworkEvents().also { f ->
      scope.launch(context = Dispatchers.Default) {
        f.collect { event ->
          when (event) {
            is BroadcastEvent.ConnectionChanged -> {
              s.connection.update { info -> info.update { it.copy(hostName = event.hostName) } }
            }
            else -> {
              // Unhandled event
            }
          }
        }
      }
    }

    // Broadcast type
    expertPreferences.listenForBroadcastType().also { f ->
      scope.launch(context = Dispatchers.Default) { f.collect { s.broadcastType.value = it } }
    }

    // Preferred Network
    expertPreferences.listenForPreferredNetwork().also { f ->
      scope.launch(context = Dispatchers.Default) { f.collect { s.preferredNetwork.value = it } }
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

        registry
            .registerProvider(KEY_SHOW_SLOW_SPEED_HELP) { state.isShowingSlowSpeedHelp.value }
            .also { add(it) }
      }

  override fun consumeRestoredState(registry: SaveableStateRegistry) {
    val s = state
    registry
        .consumeRestored(KEY_IS_SETTINGS_OPEN)
        ?.let { it.cast<Boolean>() }
        ?.also { s.isSettingsOpen.value = it }

    registry
        .consumeRestored(KEY_IS_SHOWING_QR)
        ?.let { it.cast<Boolean>() }
        ?.also { s.isShowingQRCodeDialog.value = it }

    registry
        .consumeRestored(KEY_SHOW_SLOW_SPEED_HELP)
        ?.let { it.cast<Boolean>() }
        ?.also { state.isShowingSlowSpeedHelp.value = it }
  }

  fun handleRefreshConnectionInfo(scope: CoroutineScope) {
    scope.launch(context = Dispatchers.Default) { networkUpdater.updateNetworkInfo() }
  }

  fun handleOpenSettings() {
    state.isSettingsOpen.value = true
  }

  fun handleCloseSettings() {
    state.isSettingsOpen.value = false
  }

  fun handleOpenQRCodeDialog() {
    // If the hotspot is valid, we will have this from the group
    val isHotspotDataValid = state.group.value is BroadcastNetworkStatus.GroupInfo.Connected
    state.isShowingQRCodeDialog.value = isHotspotDataValid && isNetworkCurrentlyRunning.value
  }

  fun handleCloseQRCodeDialog() {
    state.isShowingQRCodeDialog.value = false
  }

  fun handleAnalyticsMarkOpened() {
    inAppRatingPreferences.markAppOpened()
  }

  fun handleOpenSlowSpeedHelp() {
    state.isShowingSlowSpeedHelp.value = true
  }

  fun handleCloseSlowSpeedHelp() {
    state.isShowingSlowSpeedHelp.value = false
  }

  companion object {

    private const val KEY_IS_SETTINGS_OPEN = "is_settings_open"
    private const val KEY_IS_SHOWING_QR = "show_qr"

    private const val KEY_SHOW_SLOW_SPEED_HELP = "key_show_slow_speed_help"
  }
}
