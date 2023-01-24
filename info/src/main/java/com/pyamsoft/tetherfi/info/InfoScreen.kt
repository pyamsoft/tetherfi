package com.pyamsoft.tetherfi.info

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.tetherfi.ui.ServerViewState
import com.pyamsoft.tetherfi.ui.TestServerViewState
import com.pyamsoft.tetherfi.ui.renderPYDroidExtras

@Composable
fun InfoScreen(
    modifier: Modifier = Modifier,
    appName: String,
    state: InfoViewState,
    serverViewState: ServerViewState,
    onTogglePasswordVisibility: () -> Unit,
    onShowQRCode: () -> Unit,
) {
  val itemModifier = Modifier.fillMaxWidth().padding(horizontal = MaterialTheme.keylines.content)

  LazyColumn(
      modifier = modifier,
  ) {
    renderPYDroidExtras()

    renderConnectionInstructions(
        itemModifier = itemModifier,
        appName = appName,
        state = state,
        serverViewState = serverViewState,
        onTogglePasswordVisibility = onTogglePasswordVisibility,
        onShowQRCode = onShowQRCode,
    )

    item {
      Spacer(
          modifier = Modifier.padding(top = MaterialTheme.keylines.content).navigationBarsPadding(),
      )
    }
  }
}

@Preview
@Composable
private fun PreviewInfoScreen() {
  InfoScreen(
      appName = "TEST",
      state = MutableInfoViewState(),
      serverViewState = TestServerViewState(),
      onTogglePasswordVisibility = {},
      onShowQRCode = {},
  )
}
