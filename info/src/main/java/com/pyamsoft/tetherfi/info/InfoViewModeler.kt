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
    private val state: MutableInfoViewState,
    private val network: WiDiNetworkStatus,
    private val serverPreferences: ServerPreferences,
    private val wiDiReceiver: WiDiReceiver,
) : AbstractViewModeler<InfoViewState>(state) {

  fun bind(scope: CoroutineScope) {
    val s = state
    scope.launch(context = Dispatchers.Main) {
      serverPreferences.listenForPortChanges().collectLatest { s.port = it }
    }

    scope.launch(context = Dispatchers.Main) {
      wiDiReceiver.onEvent { event ->
        when (event) {
          is WidiNetworkEvent.ConnectionChanged -> {
            s.ip = event.ip
          }
          is WidiNetworkEvent.ThisDeviceChanged -> {}
          is WidiNetworkEvent.PeersChanged -> {}
          is WidiNetworkEvent.WifiDisabled -> {}
          is WidiNetworkEvent.WifiEnabled -> {}
          is WidiNetworkEvent.DiscoveryChanged -> {}
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
        s.ip = "NO IP ADDRESS"
      } else {
        s.ip = conn.ip
      }
    }

    // Pull group info
    scope.launch(context = Dispatchers.Main) {
      val grp = network.getGroupInfo()
      if (grp == null) {
        s.ssid = "NO SSID"
        s.password = "NO PASSWORD"
      } else {
        s.ssid = grp.ssid
        s.password = grp.password
      }
    }
  }
}
