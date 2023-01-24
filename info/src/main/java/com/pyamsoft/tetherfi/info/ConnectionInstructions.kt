package com.pyamsoft.tetherfi.info

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.tetherfi.ui.ServerViewState
import com.pyamsoft.tetherfi.ui.TestServerViewState

internal fun LazyListScope.renderConnectionInstructions(
    itemModifier: Modifier = Modifier,
    appName: String,
    state: InfoViewState,
    serverViewState: ServerViewState,
    onShowQRCode: () -> Unit,
    onTogglePasswordVisibility: () -> Unit,
) {
  item {
    Spacer(
        modifier = Modifier.height(MaterialTheme.keylines.content),
    )
  }

  renderDeviceIdentifiers(
      itemModifier = itemModifier,
  )

  item {
    Spacer(
        modifier = Modifier.height(MaterialTheme.keylines.content),
    )
  }

  renderAppSetup(
      itemModifier = itemModifier,
      appName = appName,
  )

  item {
    Spacer(
        modifier = Modifier.height(MaterialTheme.keylines.content),
    )
  }

  renderDeviceSetup(
      itemModifier = itemModifier,
      appName = appName,
      state = state,
      serverViewState = serverViewState,
      onTogglePasswordVisibility = onTogglePasswordVisibility,
      onShowQRCode = onShowQRCode,
  )

  item {
    Spacer(
        modifier = Modifier.height(MaterialTheme.keylines.content),
    )
  }

  renderConnectionComplete(
      itemModifier = itemModifier,
      appName = appName,
  )

  item {
    Spacer(
        modifier = Modifier.height(MaterialTheme.keylines.content),
    )
  }
}

@Preview
@Composable
private fun PreviewConnectionInstructions() {
  LazyColumn {
    renderConnectionInstructions(
        appName = "TEST",
        serverViewState = TestServerViewState(),
        state = MutableInfoViewState(),
        onTogglePasswordVisibility = {},
        onShowQRCode = {},
    )
  }
}
