package com.pyamsoft.tetherfi.connections

import com.pyamsoft.pydroid.arch.AbstractViewModeler
import com.pyamsoft.tetherfi.server.clients.BlockedClientTracker
import com.pyamsoft.tetherfi.server.clients.BlockedClients
import com.pyamsoft.tetherfi.server.clients.SeenClients
import com.pyamsoft.tetherfi.server.clients.TetherClient
import com.pyamsoft.tetherfi.server.clients.key
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class ConnectionViewModel
@Inject
internal constructor(
    override val state: MutableConnectionViewState,
    private val connections: SeenClients,
    private val blocked: BlockedClients,
    private val blockTracker: BlockedClientTracker,
) : AbstractViewModeler<ConnectionViewState>(state) {

  fun bind(scope: CoroutineScope) {
    scope.launch(context = Dispatchers.Default) {
      connections.listenForClients().collectLatest { clients ->
        val list = clients.toList().sortedBy { it.key() }
        Timber.d("New client list: $list")
        state.connections.value = list
      }
    }

    scope.launch(context = Dispatchers.Default) {
      blocked.listenForBlocked().collectLatest { clients ->
        Timber.d("New block list: $clients")
        state.blocked.value = clients
      }
    }
  }

  fun handleToggleBlock(client: TetherClient) {
    if (blocked.isBlocked(client)) {
      blockTracker.unblock(client)
    } else {
      blockTracker.block(client)
    }
  }
}
