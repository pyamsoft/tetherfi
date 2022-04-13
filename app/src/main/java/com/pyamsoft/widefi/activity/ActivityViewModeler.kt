package com.pyamsoft.widefi.activity

import com.pyamsoft.pydroid.arch.AbstractViewModeler
import com.pyamsoft.widefi.server.ConnectionEvent
import com.pyamsoft.widefi.server.widi.WiDiNetwork
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

internal class ActivityViewModeler
@Inject
internal constructor(
    private val state: MutableActivityViewState,
    private val network: WiDiNetwork,
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

            val e: ActivityEvent
            if (existing == null) {
              e = ActivityEvent.forHost(request.host)
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
