package com.pyamsoft.tetherfi.connections.sections

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.pyamsoft.pydroid.theme.keylines

private enum class RenderExcuseContentTypes {
  SORRY,
  SORRY_EXTRA,
}

internal fun LazyListScope.renderExcuse(
    modifier: Modifier = Modifier,
) {
  item(
      contentType = RenderExcuseContentTypes.SORRY,
  ) {
    Text(
        modifier = modifier.padding(vertical = MaterialTheme.keylines.content),
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
      contentType = RenderExcuseContentTypes.SORRY_EXTRA,
  ) {
    Text(
        modifier = modifier.padding(vertical = MaterialTheme.keylines.content),
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
