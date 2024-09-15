/*
 * Copyright 2024 pyamsoft
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

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.tetherfi.status.MutableStatusViewState
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
  EDIT_PORT,
}

internal fun LazyListScope.renderEditableItems(
    modifier: Modifier = Modifier,
    state: StatusViewState,
    serverViewState: ServerViewState,
    onSsidChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onPortChanged: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
) {
  item(
      contentType = RenderEditableItemsContentTypes.EDIT_SSID,
  ) {
    val isRNDISConnection by serverViewState.isRNDISConnection.collectAsStateWithLifecycle()

    if (!isRNDISConnection) {
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
    val isRNDISConnection by serverViewState.isRNDISConnection.collectAsStateWithLifecycle()

    if (!isRNDISConnection) {
      EditPassword(
          modifier = modifier.padding(bottom = MaterialTheme.keylines.baseline),
          state = state,
          onTogglePasswordVisibility = onTogglePasswordVisibility,
          onPasswordChanged = onPasswordChanged,
      )
    }
  }

  item(
      contentType = RenderEditableItemsContentTypes.EDIT_PORT,
  ) {
    EditPort(
        modifier = modifier.padding(bottom = MaterialTheme.keylines.baseline),
        state = state,
        onPortChanged = onPortChanged,
    )
  }
}

@TestOnly
@Composable
private fun PreviewEditableItems(
    ssid: String = TEST_SSID,
    password: String = TEST_PASSWORD,
    port: String = "$TEST_PORT",
) {
  LazyColumn {
    renderEditableItems(
        modifier = Modifier.width(LANDSCAPE_MAX_WIDTH),
        state =
            MutableStatusViewState().apply {
              this.ssid.value = ssid
              this.password.value = password
              this.port.value = port
            },
        onPortChanged = {},
        onSsidChanged = {},
        onPasswordChanged = {},
        onTogglePasswordVisibility = {},
        serverViewState = makeTestServerState(TestServerState.EMPTY),
    )
  }
}

@Composable
@Preview(showBackground = true)
private fun PreviewEditableItemsBlank() {
  PreviewEditableItems(
      ssid = "",
      password = "",
      port = "",
  )
}

@Composable
@Preview(showBackground = true)
private fun PreviewEditableItemsOnlySsid() {
  PreviewEditableItems(
      password = "",
      port = "",
  )
}

@Composable
@Preview(showBackground = true)
private fun PreviewEditableItemsOnlyPassword() {
  PreviewEditableItems(
      ssid = "",
      port = "",
  )
}

@Composable
@Preview(showBackground = true)
private fun PreviewEditableItemsOnlyPort() {
  PreviewEditableItems(
      ssid = "",
      password = "",
  )
}
