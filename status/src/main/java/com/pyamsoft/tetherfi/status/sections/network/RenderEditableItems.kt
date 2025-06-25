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

package com.pyamsoft.tetherfi.status.sections.network

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.tetherfi.server.broadcast.BroadcastType
import com.pyamsoft.tetherfi.status.MutableStatusViewState
import com.pyamsoft.tetherfi.status.ServerPortTypes
import com.pyamsoft.tetherfi.status.StatusViewState
import com.pyamsoft.tetherfi.ui.LANDSCAPE_MAX_WIDTH
import com.pyamsoft.tetherfi.ui.ServerViewState
import com.pyamsoft.tetherfi.ui.test.TEST_PASSWORD
import com.pyamsoft.tetherfi.ui.test.TEST_PORT
import com.pyamsoft.tetherfi.ui.test.TEST_SSID
import com.pyamsoft.tetherfi.ui.test.TestServerState
import com.pyamsoft.tetherfi.ui.test.makeTestServerState
import org.jetbrains.annotations.TestOnly

private enum class RenderEditableItemsContentTypes {
  EDIT_SSID,
  EDIT_PASSWD,
  EDIT_HTTP,
  EDIT_SOCKS,
}

internal fun LazyListScope.renderEditableItems(
    modifier: Modifier = Modifier,
    state: StatusViewState,
    serverViewState: ServerViewState,
    onSsidChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onHttpEnabledChanged: (Boolean) -> Unit,
    onHttpPortChanged: (String) -> Unit,
    onSocksEnabledChanged: (Boolean) -> Unit,
    onSocksPortChanged: (String) -> Unit,
    onEnableChangeFailed: (ServerPortTypes) -> Unit,
) {
  item(
      contentType = RenderEditableItemsContentTypes.EDIT_SSID,
  ) {
    val broadcastType by serverViewState.broadcastType.collectAsStateWithLifecycle()

    if (broadcastType == BroadcastType.WIFI_DIRECT) {
      EditSsid(
          modifier = modifier.padding(bottom = MaterialTheme.keylines.baseline),
          state = state,
          onSsidChanged = onSsidChanged,
      )
    }
  }

  item(
      contentType = RenderEditableItemsContentTypes.EDIT_PASSWD,
  ) {
    val broadcastType by serverViewState.broadcastType.collectAsStateWithLifecycle()

    if (broadcastType == BroadcastType.WIFI_DIRECT) {
      EditPassword(
          modifier = modifier.padding(bottom = MaterialTheme.keylines.baseline),
          state = state,
          onTogglePasswordVisibility = onTogglePasswordVisibility,
          onPasswordChanged = onPasswordChanged,
      )
    }
  }

  item(
      contentType = RenderEditableItemsContentTypes.EDIT_HTTP,
  ) {
    val isHttpEnabled by serverViewState.isHttpEnabled.collectAsStateWithLifecycle()
    val isSocksEnabled by serverViewState.isSocksEnabled.collectAsStateWithLifecycle()
    Row(
        modifier = modifier.padding(bottom = MaterialTheme.keylines.baseline),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      Switch(
          modifier =
              Modifier.padding(
                  start = MaterialTheme.keylines.baseline,
                  end = MaterialTheme.keylines.content,
              ),
          checked = isHttpEnabled,
          onCheckedChange = {
            if (isSocksEnabled) {
              onHttpEnabledChanged(it)
            } else {
              onEnableChangeFailed(ServerPortTypes.HTTP)
            }
          },
      )

      EditHttpPort(
          modifier = Modifier.weight(1F),
          state = state,
          onPortChanged = onHttpPortChanged,
      )
    }
  }

  item(
      contentType = RenderEditableItemsContentTypes.EDIT_SOCKS,
  ) {
    val isHttpEnabled by serverViewState.isHttpEnabled.collectAsStateWithLifecycle()
    val isSocksEnabled by serverViewState.isSocksEnabled.collectAsStateWithLifecycle()
    Row(
        modifier = modifier.padding(bottom = MaterialTheme.keylines.baseline),
        verticalAlignment = Alignment.CenterVertically,
    ) {
      Switch(
          modifier =
              Modifier.padding(
                  start = MaterialTheme.keylines.baseline,
                  end = MaterialTheme.keylines.content,
              ),
          checked = isSocksEnabled,
          onCheckedChange = {
            if (isHttpEnabled) {
              onSocksEnabledChanged(it)
            } else {
              onEnableChangeFailed(ServerPortTypes.SOCKS)
            }
          },
      )

      EditSocksPort(
          modifier = Modifier.weight(1F),
          state = state,
          onPortChanged = onSocksPortChanged,
      )
    }
  }
}

@TestOnly
@Composable
private fun PreviewEditableItems(
    ssid: String = TEST_SSID,
    password: String = TEST_PASSWORD,
    port: String = "$TEST_PORT",
    http: Boolean,
    socks: Boolean,
) {
  LazyColumn {
    renderEditableItems(
        modifier = Modifier.width(LANDSCAPE_MAX_WIDTH),
        state =
            MutableStatusViewState().apply {
              this.ssid.value = ssid
              this.password.value = password
              this.httpPort.value = port
            },
        onHttpEnabledChanged = {},
        onHttpPortChanged = {},
        onSocksEnabledChanged = {},
        onSocksPortChanged = {},
        onSsidChanged = {},
        onPasswordChanged = {},
        onTogglePasswordVisibility = {},
        onEnableChangeFailed = {},
        serverViewState = makeTestServerState(TestServerState.EMPTY, http, socks),
    )
  }
}

@Composable
@Preview(showBackground = true)
private fun PreviewEditableItemsBlankHttp() {
  PreviewEditableItems(
      ssid = "",
      password = "",
      port = "",
      http = true,
      socks = false,
  )
}

@Composable
@Preview(showBackground = true)
private fun PreviewEditableItemsOnlySsidHttp() {
  PreviewEditableItems(
      password = "",
      port = "",
      http = true,
      socks = false,
  )
}

@Composable
@Preview(showBackground = true)
private fun PreviewEditableItemsOnlyPasswordHttp() {
  PreviewEditableItems(
      ssid = "",
      port = "",
      http = true,
      socks = false,
  )
}

@Composable
@Preview(showBackground = true)
private fun PreviewEditableItemsOnlyPortHttp() {
  PreviewEditableItems(
      ssid = "",
      password = "",
      http = true,
      socks = false,
  )
}

@Composable
@Preview(showBackground = true)
private fun PreviewEditableItemsBlankSocks() {
  PreviewEditableItems(
      ssid = "",
      password = "",
      port = "",
      http = false,
      socks = true,
  )
}

@Composable
@Preview(showBackground = true)
private fun PreviewEditableItemsOnlySsidSocks() {
  PreviewEditableItems(
      password = "",
      port = "",
      http = false,
      socks = true,
  )
}

@Composable
@Preview(showBackground = true)
private fun PreviewEditableItemsOnlyPasswordSocks() {
  PreviewEditableItems(
      ssid = "",
      port = "",
      http = false,
      socks = true,
  )
}

@Composable
@Preview(showBackground = true)
private fun PreviewEditableItemsOnlyPortSocks() {
  PreviewEditableItems(
      ssid = "",
      password = "",
      http = false,
      socks = true,
  )
}

@Composable
@Preview(showBackground = true)
private fun PreviewEditableItemsBlankBoth() {
  PreviewEditableItems(
      ssid = "",
      password = "",
      port = "",
      http = true,
      socks = true,
  )
}

@Composable
@Preview(showBackground = true)
private fun PreviewEditableItemsOnlySsidBoth() {
  PreviewEditableItems(
      password = "",
      port = "",
      http = true,
      socks = true,
  )
}

@Composable
@Preview(showBackground = true)
private fun PreviewEditableItemsOnlyPasswordBoth() {
  PreviewEditableItems(
      ssid = "",
      port = "",
      http = true,
      socks = true,
  )
}

@Composable
@Preview(showBackground = true)
private fun PreviewEditableItemsOnlyPortBoth() {
  PreviewEditableItems(
      ssid = "",
      password = "",
      http = true,
      socks = true,
  )
}
