package com.pyamsoft.tetherfi.error

import com.pyamsoft.pydroid.arch.AbstractViewModeler
import com.pyamsoft.tetherfi.server.event.ErrorEvent
import com.pyamsoft.tetherfi.server.widi.WiDiNetwork
import com.pyamsoft.tetherfi.server.widi.WiDiNetworkStatus
import com.pyamsoft.tetherfi.ui.ProxyEvent
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class ErrorViewModeler
@Inject
internal constructor(
    private val state: MutableErrorViewState,
    private val network: WiDiNetworkStatus,
) : AbstractViewModeler<ErrorViewState>(state) {

  fun watchNetworkActivity(scope: CoroutineScope) {
    scope.launch(context = Dispatchers.Main) {
      val s = state
      network.onErrorEvent { event ->
        when (event) {
          is ErrorEvent.Clear -> {
            s.events = emptyList()
          }
          is ErrorEvent.Tcp -> {
            val request = event.request
            if (request == null) {
              Timber.w("Error event with no request data: $event")
              return@onErrorEvent
            }

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
          is ErrorEvent.Udp -> {}
        }
      }
    }
  }
}
