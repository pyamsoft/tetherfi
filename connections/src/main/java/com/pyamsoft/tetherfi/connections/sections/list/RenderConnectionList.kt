/*
 * Copyright 2025 pyamsoft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pyamsoft.tetherfi.connections.sections.list

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.tetherfi.connections.R
import com.pyamsoft.tetherfi.server.broadcast.BroadcastNetworkStatus
import com.pyamsoft.tetherfi.server.clients.TetherClient
import com.pyamsoft.tetherfi.server.clients.key

private enum class RenderConnectionListContentTypes {
  EMPTY,
  HEADER,
  CLIENT,
  START,
}

internal fun LazyListScope.renderConnectionList(
    modifier: Modifier = Modifier,
    group: BroadcastNetworkStatus.GroupInfo,
    clients: List<TetherClient>,
    blocked: List<TetherClient>,
    onToggleBlock: (TetherClient) -> Unit,
    onManageNickName: (TetherClient) -> Unit,
    onManageTransferLimit: (TetherClient) -> Unit,
    onManageBandwidthLimit: (TetherClient) -> Unit,
) {
  group.also { gi ->
    if (gi is BroadcastNetworkStatus.GroupInfo.Connected) {
      if (clients.isEmpty()) {
        renderRunningNoClients(
            modifier = modifier,
        )
      } else {
        renderRunningWithClients(
            itemModifier = modifier,
            clients = clients,
            blocked = blocked,
            onToggleBlock = onToggleBlock,
            onManageNickName = onManageNickName,
            onManageTransferLimit = onManageTransferLimit,
            onManageBandwidthLimit = onManageBandwidthLimit,
        )
      }
    } else {
      renderNotRunning(
          modifier = modifier,
      )
    }
  }
}

private fun LazyListScope.renderRunningNoClients(
    modifier: Modifier = Modifier,
) {
  item(
      contentType = RenderConnectionListContentTypes.EMPTY,
  ) {
    Text(
        modifier =
            modifier
                .padding(vertical = MaterialTheme.keylines.content)
                .padding(top = MaterialTheme.keylines.content * 3),
        text = stringResource(R.string.connection_none),
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Center,
    )
  }
}

private fun LazyListScope.renderRunningWithClients(
    itemModifier: Modifier = Modifier,
    clients: List<TetherClient>,
    blocked: List<TetherClient>,
    onToggleBlock: (TetherClient) -> Unit,
    onManageNickName: (TetherClient) -> Unit,
    onManageTransferLimit: (TetherClient) -> Unit,
    onManageBandwidthLimit: (TetherClient) -> Unit,
) {
  item(
      contentType = RenderConnectionListContentTypes.HEADER,
  ) {
    Text(
        modifier =
            itemModifier
                .padding(vertical = MaterialTheme.keylines.content)
                .padding(top = MaterialTheme.keylines.content * 3),
        text = stringResource(R.string.connection_running_explain),
        style =
            MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onBackground,
            ),
        textAlign = TextAlign.Center,
    )
  }

  items(
      items = clients,
      key = { it.key() },
      contentType = { RenderConnectionListContentTypes.CLIENT },
  ) { client ->
    ConnectionItem(
        modifier = itemModifier,
        client = client,
        blocked = blocked,
        onToggleBlock = onToggleBlock,
        onManageNickName = onManageNickName,
        onManageTransferLimit = onManageTransferLimit,
        onManageBandwidthLimit = onManageBandwidthLimit,
    )
  }
}

private fun LazyListScope.renderNotRunning(modifier: Modifier = Modifier) {
  item(
      contentType = RenderConnectionListContentTypes.START,
  ) {
    Text(
        modifier =
            modifier
                .padding(vertical = MaterialTheme.keylines.content)
                .padding(top = MaterialTheme.keylines.content * 3),
        text = stringResource(R.string.connection_start_before_manage),
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Center,
    )
  }
}
