package com.pyamsoft.widefi.status

import com.pyamsoft.pydroid.arch.AbstractViewModeler
import com.pyamsoft.widefi.server.ServerDefaults
import com.pyamsoft.widefi.server.ServerPreferences
import com.pyamsoft.widefi.server.status.RunningStatus
import com.pyamsoft.widefi.server.widi.WiDiNetwork
import com.pyamsoft.widefi.server.widi.receiver.WidiNetworkEvent
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class StatusViewModeler
@Inject
internal constructor(
    private val preferences: ServerPreferences,
    private val state: MutableStatusViewState,
    private val network: WiDiNetwork,
) : AbstractViewModeler<StatusViewState>(state) {

  private fun toggleProxyState(
      onStart: () -> Unit,
      onStop: () -> Unit,
  ) {
    when (val s = network.getCurrentStatus()) {
      is RunningStatus.NotRunning -> network.start(onStart)
      is RunningStatus.Running -> network.stop(onStop)
      is RunningStatus.Error -> network.stop(onStop)
      else -> {
        Timber.d("Cannot toggle while we are in the middle of an operation: $s")
      }
    }
  }

  fun loadPreferences(scope: CoroutineScope) {
    val s = state
    if (s.preferencesLoaded) {
      return
    }

    scope.launch(context = Dispatchers.Main) {
      s.ssid = preferences.getSsid()
      s.password = preferences.getPassword()
      s.port = preferences.getPort()
      s.band = preferences.getNetworkBand()

      s.preferencesLoaded = true
    }
  }

  fun refreshGroupInfo(scope: CoroutineScope) {
    scope.launch(context = Dispatchers.Main) { state.group = network.getGroupInfo() }
  }

  fun watchStatusUpdates(scope: CoroutineScope) {
    scope.launch(context = Dispatchers.Main) {
      network.onProxyStatusChanged { state.proxyStatus = it }
    }

    scope.launch(context = Dispatchers.Main) { network.onStatusChanged { state.wiDiStatus = it } }

    scope.launch(context = Dispatchers.Main) {
      network.onWifiDirectEvent { event ->
        when (event) {
          is WidiNetworkEvent.ConnectionChanged -> {
            Timber.d("Connection Changed, refresh group info")
            state.ip = event.ip
            refreshGroupInfo(scope = scope)
          }
          is WidiNetworkEvent.ThisDeviceChanged -> {
            Timber.d("This Device Changed, refresh group info")
            refreshGroupInfo(scope = scope)
          }
          is WidiNetworkEvent.PeersChanged -> {
            Timber.d("Peers Changed, refresh group info")
            refreshGroupInfo(scope = scope)
          }
          is WidiNetworkEvent.WifiDisabled -> {
            Timber.d("Wifi Disabled, refresh group info")
            refreshGroupInfo(scope = scope)
          }
          is WidiNetworkEvent.WifiEnabled -> {
            Timber.d("Wifi Enabled, refresh group info")
            refreshGroupInfo(scope = scope)
          }
          is WidiNetworkEvent.DiscoveryChanged -> {
            Timber.d("Discovery changed, refresh group info")
            refreshGroupInfo(scope = scope)
          }
        }
      }
    }
  }

  fun handleToggleProxy(
      scope: CoroutineScope,
      onStart: () -> Unit,
      onStop: () -> Unit,
  ) {
    toggleProxyState(
        onStart = onStart,
        onStop = onStop,
    )
    refreshGroupInfo(scope = scope)
  }
}
