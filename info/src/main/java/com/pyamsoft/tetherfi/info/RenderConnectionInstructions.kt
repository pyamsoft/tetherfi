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

package com.pyamsoft.tetherfi.info

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.tetherfi.core.FeatureFlags
import com.pyamsoft.tetherfi.info.sections.renderAppSetup
import com.pyamsoft.tetherfi.info.sections.renderConnectionComplete
import com.pyamsoft.tetherfi.info.sections.renderDeviceIdentifiers
import com.pyamsoft.tetherfi.info.sections.renderDeviceSetup
import com.pyamsoft.tetherfi.ui.ServerViewState
import com.pyamsoft.tetherfi.ui.test.TestServerState
import com.pyamsoft.tetherfi.ui.test.makeTestFeatureFlags
import com.pyamsoft.tetherfi.ui.test.makeTestServerState
import org.jetbrains.annotations.TestOnly

private enum class ConnectionInstructionContentTypes {
  SPACER,
}

internal fun LazyListScope.renderConnectionInstructions(
    itemModifier: Modifier = Modifier,
    appName: String,
    featureFlags: FeatureFlags,
    state: InfoViewState,
    serverViewState: ServerViewState,
    onShowQRCode: () -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onShowSlowSpeedHelp: () -> Unit,
    onToggleShowOptions: (InfoViewOptionsType) -> Unit,
) {
  item(
      contentType = ConnectionInstructionContentTypes.SPACER,
  ) {
    Spacer(
        modifier = Modifier.height(MaterialTheme.keylines.content),
    )
  }

  renderDeviceIdentifiers(
      itemModifier = itemModifier,
  )

  item(
      contentType = ConnectionInstructionContentTypes.SPACER,
  ) {
    Spacer(
        modifier = Modifier.height(MaterialTheme.keylines.content),
    )
  }

  renderAppSetup(
      itemModifier = itemModifier,
      appName = appName,
      serverViewState = serverViewState,
  )

  item(
      contentType = ConnectionInstructionContentTypes.SPACER,
  ) {
    Spacer(
        modifier = Modifier.height(MaterialTheme.keylines.content),
    )
  }

  renderDeviceSetup(
      itemModifier = itemModifier,
      appName = appName,
      state = state,
      featureFlags = featureFlags,
      serverViewState = serverViewState,
      onTogglePasswordVisibility = onTogglePasswordVisibility,
      onShowQRCode = onShowQRCode,
      onToggleShowOptions = onToggleShowOptions,
  )

  item(
      contentType = ConnectionInstructionContentTypes.SPACER,
  ) {
    Spacer(
        modifier = Modifier.height(MaterialTheme.keylines.content),
    )
  }

  renderConnectionComplete(
      itemModifier = itemModifier,
      appName = appName,
      onShowSlowSpeedHelp = onShowSlowSpeedHelp,
  )

  item(
      contentType = ConnectionInstructionContentTypes.SPACER,
  ) {
    Spacer(
        modifier = Modifier.height(MaterialTheme.keylines.content),
    )
  }
}

@TestOnly
@Composable
private fun PreviewConnectionInstructions(state: InfoViewState, server: TestServerState) {
  LazyColumn {
    renderConnectionInstructions(
        appName = "TEST",
        featureFlags = makeTestFeatureFlags(),
        serverViewState = makeTestServerState(server),
        state = state,
        onTogglePasswordVisibility = {},
        onShowQRCode = {},
        onShowSlowSpeedHelp = {},
        onToggleShowOptions = {},
    )
  }
}

@Composable
@Preview(showBackground = true)
private fun PreviewConnectionInstructionsEmpty() {
  PreviewConnectionInstructions(state = MutableInfoViewState(), server = TestServerState.EMPTY)
}

@Composable
@Preview(showBackground = true)
private fun PreviewConnectionInstructionsActive() {
  PreviewConnectionInstructions(state = MutableInfoViewState(), server = TestServerState.CONNECTED)
}

@Composable
@Preview(showBackground = true)
private fun PreviewConnectionInstructionsActivePassword() {
  PreviewConnectionInstructions(
      state = MutableInfoViewState().apply { isPasswordVisible.value = true },
      server = TestServerState.CONNECTED)
}
