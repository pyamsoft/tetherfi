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

package com.pyamsoft.tetherfi.status.sections.tiles

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.tetherfi.server.broadcast.BroadcastNetworkStatus
import com.pyamsoft.tetherfi.ui.ServerViewState

@Composable
internal fun RunningTiles(
    modifier: Modifier = Modifier,
    serverViewState: ServerViewState,

    // Connections
    onShowQRCode: () -> Unit,
    onRefreshConnection: () -> Unit,

    // Errors
    onShowNetworkError: () -> Unit,
    onShowHotspotError: () -> Unit,
) {
  val group by serverViewState.group.collectAsStateWithLifecycle()
  val connection by serverViewState.connection.collectAsStateWithLifecycle()

  val isQREnabled =
      remember(
          connection,
          group,
      ) {
        connection is BroadcastNetworkStatus.ConnectionInfo.Connected &&
            group is BroadcastNetworkStatus.GroupInfo.Connected
      }

  Column(
      modifier = modifier.padding(vertical = MaterialTheme.keylines.content),
  ) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = MaterialTheme.keylines.content),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      RefreshTile(
          modifier = Modifier.weight(1F),
          onRefreshConnection = onRefreshConnection,
      )

      Spacer(
          modifier = Modifier.width(MaterialTheme.keylines.content),
      )

      ViewQRCodeTile(
          modifier = Modifier.weight(1F),
          isQREnabled = isQREnabled,
          onShowQRCode = onShowQRCode,
      )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      ConnectionErrorTile(
          modifier = Modifier.weight(1F),
          connection = connection,
          onShowConnectionError = onShowNetworkError,
      )

      if (connection is BroadcastNetworkStatus.ConnectionInfo.Error &&
          group is BroadcastNetworkStatus.GroupInfo.Error) {
        Spacer(
            modifier = Modifier.width(MaterialTheme.keylines.content),
        )
      }

      GroupErrorTile(
          modifier = Modifier.weight(1F),
          group = group,
          onShowGroupError = onShowHotspotError,
      )
    }
  }
}
