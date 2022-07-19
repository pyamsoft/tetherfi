package com.pyamsoft.tetherfi.activity

import com.pyamsoft.pydroid.arch.AbstractViewModeler
import com.pyamsoft.tetherfi.server.event.ConnectionEvent
import com.pyamsoft.tetherfi.server.widi.WiDiNetworkStatus
import com.pyamsoft.tetherfi.ui.event.ProxyEvent
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ActivityViewModeler
@Inject
internal constructor(
    private val state: MutableActivityViewState,
    private val network: WiDiNetworkStatus,
) : AbstractViewModeler<ActivityViewState>(state) {

  fun watchNetworkActivity(scope: CoroutineScope) {
    scope.launch(context = Dispatchers.Main) {
      val s = state

      network.onConnectionEvent { event ->
        when (event) {
          is ConnectionEvent.Clear -> {
            s.events = emptyList()
          }
          is ConnectionEvent.Tcp -> {
            val request = event.request
            val newEvents = s.events.toMutableList()
            val existing = newEvents.find { it.host == request.host }

            val e: ProxyEvent
            if (existing == null) {
              e = ProxyEvent.forHost(request.host)
            } else {
              e = existing
              newEvents.remove(existing)
            }

            val newE =
                e.addTcpConnection(
                    url = request.url,
                    method = request.method,
                )
            newEvents.add(newE)
            s.events = newEvents
          }
          is ConnectionEvent.Udp -> {}
        }
      }
    }
  }
}
