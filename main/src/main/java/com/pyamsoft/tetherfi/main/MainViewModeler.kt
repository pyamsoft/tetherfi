package com.pyamsoft.tetherfi.main

import androidx.compose.runtime.saveable.SaveableStateRegistry
import com.pyamsoft.pydroid.arch.AbstractViewModeler
import com.pyamsoft.tetherfi.server.ServerPreferences
import com.pyamsoft.tetherfi.server.status.RunningStatus
import com.pyamsoft.tetherfi.server.widi.WiDiNetworkStatus
import com.pyamsoft.tetherfi.server.widi.receiver.WiDiReceiver
import com.pyamsoft.tetherfi.server.widi.receiver.WidiNetworkEvent
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

class MainViewModeler
@Inject
internal constructor(
    override val state: MutableMainViewState,
    private val network: WiDiNetworkStatus,
    private val serverPreferences: ServerPreferences,
    private val wiDiReceiver: WiDiReceiver,
) : AbstractViewModeler<MainViewState>(state) {

  private val isNetworkCurrentlyRunning =
      MutableStateFlow(network.getCurrentStatus() == RunningStatus.Running)

  private fun refreshGroupInfo(scope: CoroutineScope) {
    val s = state

    scope.launch(context = Dispatchers.Main) {
      val grp = network.getGroupInfo()
      if (grp == null) {
        // Blank out since we have no valid data
        state.ssid.value = ""
        state.password.value = ""
      } else {
        s.ssid.value = grp.ssid
        s.password.value = grp.password
      }
    }
  }

  fun bind(scope: CoroutineScope) {
    val s = state

    // Watch the server status and update if it is running
    scope.launch(context = Dispatchers.Main) {
      network.onStatusChanged { s ->
        val wasRunning = isNetworkCurrentlyRunning.value
        val currentlyRunning = s == RunningStatus.Running
        isNetworkCurrentlyRunning.value = currentlyRunning

        // If the network was switched off, clear everything
        if (wasRunning && !currentlyRunning) {
          Timber.d("Hotspot was turned OFF, refresh network settings to clear")

          // Refresh connection info, should blank out
          handleRefreshConnectionInfo(scope = this)

          // Explicitly close the QR code
          handleCloseQRCodeDialog()
        } else if (!wasRunning && currentlyRunning) {
          Timber.d("Hotspot was turned ON, refresh network settings to update")

          // Refresh connection info, should populate
          handleRefreshConnectionInfo(scope = this)
        }
      }
    }

    // Port is its own thing, not part of group info
    scope.launch(context = Dispatchers.Main) {
      serverPreferences.listenForPortChanges().collectLatest { s.port.value = it }
    }

    // But then once we are done editing and we start getting events from the receiver, take them
    // instead
    scope.launch(context = Dispatchers.Main) {
      wiDiReceiver.onEvent { event ->
        when (event) {
          is WidiNetworkEvent.ConnectionChanged -> {
            s.ip.value = event.ip
            handleRefreshConnectionInfo(scope)
          }
          is WidiNetworkEvent.ThisDeviceChanged -> {
            handleRefreshConnectionInfo(scope)
          }
          is WidiNetworkEvent.PeersChanged -> {
            handleRefreshConnectionInfo(scope)
          }
          is WidiNetworkEvent.WifiDisabled -> {
            handleRefreshConnectionInfo(scope)
          }
          is WidiNetworkEvent.WifiEnabled -> {
            handleRefreshConnectionInfo(scope)
          }
          is WidiNetworkEvent.DiscoveryChanged -> {
            handleRefreshConnectionInfo(scope)
          }
        }
      }
    }
  }

  fun handleRefreshConnectionInfo(scope: CoroutineScope) {
    val s = state

    // Pull connection info
    scope.launch(context = Dispatchers.Main) {
      val conn = network.getConnectionInfo()
      if (conn == null) {
        // Blank so we handle in the View
        s.ip.value = ""
      } else {
        s.ip.value = conn.ip
      }
    }

    // Pull group info
    refreshGroupInfo(scope)
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

  fun handleOpenSettings() {
    state.isSettingsOpen.value = true
  }

  fun handleCloseSettings() {
    state.isSettingsOpen.value = false
  }

  fun handleOpenQRCodeDialog() {
    // If the hotspot is valid, we will have this from the group
    val isHotspotDataValid = state.ssid.value.isNotBlank() && state.password.value.isNotBlank()
    state.isShowingQRCodeDialog.value = isHotspotDataValid && isNetworkCurrentlyRunning.value
  }

  fun handleCloseQRCodeDialog() {
    state.isShowingQRCodeDialog.value = false
  }

  companion object {

    private const val KEY_IS_SETTINGS_OPEN = "is_settings_open"
    private const val KEY_IS_SHOWING_QR = "show_qr"
  }
}
