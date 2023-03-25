/*
 * Copyright 2023 pyamsoft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
