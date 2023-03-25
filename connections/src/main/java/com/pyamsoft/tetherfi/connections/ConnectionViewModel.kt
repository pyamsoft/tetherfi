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
        state.connections.value = clients.toList().sortedBy { it.key() }
      }
    }

    scope.launch(context = Dispatchers.Default) {
      blocked.listenForBlocked().collectLatest { state.blocked.value = it }
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
