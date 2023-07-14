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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.pydroid.ui.util.collectAsStateList
import com.pyamsoft.tetherfi.server.clients.TetherClient
import com.pyamsoft.tetherfi.ui.ServerViewState
import com.pyamsoft.tetherfi.ui.renderLinks
import com.pyamsoft.tetherfi.ui.renderPYDroidExtras

@Composable
fun ConnectionScreen(
    modifier: Modifier = Modifier,
    appName: String,
    state: ConnectionViewState,
    serverViewState: ServerViewState,
    onToggleBlock: (TetherClient) -> Unit,
) {
  val group by serverViewState.group.collectAsState()
  val clients = state.connections.collectAsStateList()
  val blocked = state.blocked.collectAsStateList()

  Scaffold(
      modifier = modifier,
  ) { pv ->
    LazyColumn(
        contentPadding = PaddingValues(horizontal = MaterialTheme.keylines.content),
    ) {
      renderPYDroidExtras()

      renderList(
          group = group,
          clients = clients,
          blocked = blocked,
          onToggleBlock = onToggleBlock,
      )

      renderExcuse()

      renderLinks(
          appName = appName,
      )

      item(
          contentType = ConnectionScreenContentTypes.BOTTOM_SPACER,
      ) {
        Spacer(
            modifier = Modifier.navigationBarsPadding().padding(pv),
        )
      }
    }
  }
}

private fun LazyListScope.renderExcuse() {
  item(
      contentType = ConnectionScreenContentTypes.SORRY,
  ) {
    Text(
        modifier = Modifier.fillMaxWidth().padding(vertical = MaterialTheme.keylines.content),
        text =
            "Sorry in advance. The Operating System does not let me see which connected device is which, so this screen can only allow you to manage things by IP address.",
        style =
            MaterialTheme.typography.body2.copy(
                color =
                    MaterialTheme.colors.onBackground.copy(
                        alpha = ContentAlpha.disabled,
                    ),
            ),
        textAlign = TextAlign.Center,
    )
  }

  item(
      contentType = ConnectionScreenContentTypes.SORRY_EXTRA,
  ) {
    Text(
        modifier = Modifier.fillMaxWidth().padding(vertical = MaterialTheme.keylines.content),
        text = "A more friendly solution is being actively investigated.",
        style =
            MaterialTheme.typography.body2.copy(
                color =
                    MaterialTheme.colors.onBackground.copy(
                        alpha = ContentAlpha.disabled,
                    ),
            ),
        textAlign = TextAlign.Center,
    )
  }
}
