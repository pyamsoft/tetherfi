package com.pyamsoft.widefi.error

import com.pyamsoft.pydroid.arch.AbstractViewModeler
import com.pyamsoft.widefi.server.event.ErrorEvent
import com.pyamsoft.widefi.server.widi.WiDiNetwork
import com.pyamsoft.widefi.ui.ProxyEvent
import com.pyamsoft.widefi.ui.logging.LogStorage
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class ErrorViewModeler
@Inject
internal constructor(
    private val logStorage: LogStorage<ErrorEvent>,
    private val state: MutableErrorViewState,
    private val network: WiDiNetwork,
) : AbstractViewModeler<ErrorViewState>(state) {

  fun watchNetworkActivity(scope: CoroutineScope) {
    scope.launch(context = Dispatchers.Main) { network.onErrorEvent { logStorage.submit(it) } }

    scope.launch(context = Dispatchers.Main) {
      val s = state
      logStorage.onLogEvent { event ->
        when (event) {
          is ErrorEvent.Clear -> {
            s.events = emptyList()
            logStorage.clear()
          }
          is ErrorEvent.Tcp -> {
            val request = event.request
            if (request == null) {
              Timber.w("Error event with no request data: $event")
              return@onLogEvent
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
