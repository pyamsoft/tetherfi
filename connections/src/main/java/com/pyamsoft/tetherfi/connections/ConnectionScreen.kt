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

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.pydroid.ui.util.collectAsStateListWithLifecycle
import com.pyamsoft.tetherfi.connections.sections.list.renderConnectionList
import com.pyamsoft.tetherfi.connections.sections.renderExcuse
import com.pyamsoft.tetherfi.server.clients.TetherClient
import com.pyamsoft.tetherfi.ui.LANDSCAPE_MAX_WIDTH
import com.pyamsoft.tetherfi.ui.ServerViewState
import com.pyamsoft.tetherfi.ui.renderLinks
import com.pyamsoft.tetherfi.ui.renderPYDroidExtras

private enum class ConnectionScreenContentTypes {
  BOTTOM_SPACER,
}

@Composable
fun ConnectionScreen(
    modifier: Modifier = Modifier,
    appName: String,
    state: ConnectionViewState,
    serverViewState: ServerViewState,
    onToggleBlock: (TetherClient) -> Unit,
) {
  val group by serverViewState.group.collectAsStateWithLifecycle()
  val clients = state.connections.collectAsStateListWithLifecycle()
  val blocked = state.blocked.collectAsStateListWithLifecycle()

  LazyColumn(
      modifier = modifier,
      contentPadding = PaddingValues(horizontal = MaterialTheme.keylines.content),
      horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    renderPYDroidExtras(
        modifier = Modifier.widthIn(max = LANDSCAPE_MAX_WIDTH),
    )

    renderConnectionList(
        modifier = Modifier.widthIn(max = LANDSCAPE_MAX_WIDTH),
        group = group,
        clients = clients,
        blocked = blocked,
        onToggleBlock = onToggleBlock,
    )

    renderExcuse(
        modifier = Modifier.widthIn(max = LANDSCAPE_MAX_WIDTH),
    )

    renderLinks(
        modifier = Modifier.widthIn(max = LANDSCAPE_MAX_WIDTH),
        appName = appName,
    )

    item(
        contentType = ConnectionScreenContentTypes.BOTTOM_SPACER,
    ) {
      Spacer(
          modifier = Modifier.navigationBarsPadding(),
      )
    }
  }
}
