package com.pyamsoft.tetherfi.info

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.tetherfi.ui.icons.renderPYDroidExtras

@Composable
fun InfoScreen(
    modifier: Modifier = Modifier,
    appName: String,
    state: InfoViewState,
) {
  val itemModifier =
      Modifier.fillMaxWidth()
          .padding(top = MaterialTheme.keylines.content)
          .padding(horizontal = MaterialTheme.keylines.content)

  LazyColumn(
      modifier = modifier,
  ) {
    renderPYDroidExtras()

    renderConnectionInstructions(
        itemModifier = itemModifier,
        appName = appName,
        state = state,
    )
  }
}

@Preview
@Composable
private fun PreviewInfoScreen() {
  InfoScreen(
      appName = "TEST",
      state =
          MutableInfoViewState().apply {
            ip = "192.168.0.1"
            ssid = "TEST NETWORK"
            password = "TEST PASSWORD"
            port = 8228
          },
  )
}
