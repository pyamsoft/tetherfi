package com.pyamsoft.tetherfi.info

import com.pyamsoft.pydroid.arch.AbstractViewModeler
import com.pyamsoft.tetherfi.server.ServerPreferences
import com.pyamsoft.tetherfi.server.widi.WiDiNetworkStatus
import com.pyamsoft.tetherfi.server.widi.receiver.WiDiReceiver
import com.pyamsoft.tetherfi.server.widi.receiver.WidiNetworkEvent
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class InfoViewModeler
@Inject
internal constructor(
    override val state: MutableInfoViewState,
    private val network: WiDiNetworkStatus,
    private val serverPreferences: ServerPreferences,
    private val wiDiReceiver: WiDiReceiver,
) : AbstractViewModeler<InfoViewState>(state) {

  private fun refreshGroupInfo(scope: CoroutineScope) {
    val s = state

    scope.launch(context = Dispatchers.Main) {
      val grp = network.getGroupInfo()
      if (grp == null) {
        s.ssid.value = "NO SSID"
        s.password.value = "NO PASSWORD"
      } else {
        s.ssid.value = grp.ssid
        s.password.value = grp.password
      }
    }
  }

  fun bind(scope: CoroutineScope) {
    val s = state
    scope.launch(context = Dispatchers.Main) {
      serverPreferences.listenForPortChanges().collectLatest { s.port.value = it }
    }

    scope.launch(context = Dispatchers.Main) {
      wiDiReceiver.onEvent { event ->
        when (event) {
          is WidiNetworkEvent.ConnectionChanged -> {
            refreshGroupInfo(scope)
            s.ip.value = event.ip
          }
          is WidiNetworkEvent.ThisDeviceChanged -> {
            refreshGroupInfo(scope)
          }
          is WidiNetworkEvent.PeersChanged -> {
            refreshGroupInfo(scope)
          }
          is WidiNetworkEvent.WifiDisabled -> {
            refreshGroupInfo(scope)
          }
          is WidiNetworkEvent.WifiEnabled -> {
            refreshGroupInfo(scope)
          }
          is WidiNetworkEvent.DiscoveryChanged -> {
            refreshGroupInfo(scope)
          }
        }
      }
    }

    refreshConnectionInfo(scope = scope)
  }

  fun refreshConnectionInfo(scope: CoroutineScope) {
    val s = state

    // Pull connection info
    scope.launch(context = Dispatchers.Main) {
      val conn = network.getConnectionInfo()
      if (conn == null) {
        s.ip.value = "NO IP ADDRESS"
      } else {
        s.ip.value = conn.ip
      }
    }

    // Pull group info
    refreshGroupInfo(scope)
  }
}
