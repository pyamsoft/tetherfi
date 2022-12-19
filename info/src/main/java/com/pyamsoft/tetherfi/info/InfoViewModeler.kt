package com.pyamsoft.tetherfi.info

import com.pyamsoft.pydroid.arch.AbstractViewModeler
import com.pyamsoft.tetherfi.server.ServerPreferences
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
    private val serverPreferences: ServerPreferences,
    private val wiDiReceiver: WiDiReceiver,
) : AbstractViewModeler<InfoViewState>(state) {

  fun bind(scope: CoroutineScope) {
    val s = state
    scope.launch(context = Dispatchers.Main) {
      serverPreferences.listenForPortChanges().collectLatest { s.port = it }
    }

    scope.launch(context = Dispatchers.Main) {
      serverPreferences.listenForSsidChanges().collectLatest { s.ssid = it }
    }

    scope.launch(context = Dispatchers.Main) {
      serverPreferences.listenForPasswordChanges().collectLatest { s.password = it }
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
  }
}
