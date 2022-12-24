package com.pyamsoft.tetherfi.info

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.pyamsoft.pydroid.theme.keylines

internal fun LazyListScope.renderConnectionComplete(
    itemModifier: Modifier = Modifier,
    appName: String,
) {
  item {
    ThisInstruction(
        modifier = itemModifier,
    ) {
      Text(
          text = "Your device should now be sharing its Internet connection!",
          style = MaterialTheme.typography.body1,
      )
    }
  }

  item {
    OtherInstruction(
        modifier = itemModifier.padding(top = MaterialTheme.keylines.content),
    ) {
      Text(
          text =
              "At this point, normal Internet browsing and email should work. If it does not, disconnect from the $appName Hotspot and double-check that you have entered the correct Network and Proxy settings.",
          style =
              MaterialTheme.typography.body2.copy(
                  color =
                      MaterialTheme.colors.onBackground.copy(
                          alpha = ContentAlpha.medium,
                      ),
              ),
      )
    }
  }
}

@Preview
@Composable
private fun PreviewConnectionComplete() {
  LazyColumn {
    renderConnectionComplete(
        appName = "TEST",
    )
  }
}
