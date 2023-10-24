package com.pyamsoft.tetherfi.status.trouble

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.pyamsoft.pydroid.theme.keylines

@Composable
internal fun TroubleshootUnableToStart(
    modifier: Modifier = Modifier,
    appName: String,
    isBroadcastError: Boolean,
    isProxyError: Boolean,
) {
  val errType =
      remember(
          isBroadcastError,
          isProxyError,
      ) {
        if (isBroadcastError && isProxyError) {
          "with your device and configuration"
        } else if (isProxyError) {
          "with your configuration"
        } else {
          "from your device"
        }
      }

  Column(
      modifier = modifier.padding(horizontal = MaterialTheme.keylines.content),
  ) {
    Text(
        text = "$appName Hotspot failed to start.",
        color = MaterialTheme.colors.error,
        style = MaterialTheme.typography.h6,
    )
    Text(
        modifier = Modifier.padding(bottom = MaterialTheme.keylines.content),
        text = "This is NOT an error with the app, this is an error $errType.",
        fontWeight = FontWeight.W700,
        style = MaterialTheme.typography.body2,
        color =
            MaterialTheme.colors.error.copy(
                alpha = ContentAlpha.medium,
            ),
    )

    Text(
        modifier = Modifier.padding(bottom = MaterialTheme.keylines.baseline),
        text = "Please check these issues and try again:",
        style = MaterialTheme.typography.caption,
        fontWeight = FontWeight.W700,
        color =
            MaterialTheme.colors.onSurface.copy(
                alpha = ContentAlpha.medium,
            ),
    )

    if (isBroadcastError) {
      Text(
          modifier = Modifier.padding(bottom = MaterialTheme.keylines.baseline),
          text = "• Wi-Fi must be turned ON to start the Hotspot",
          style = MaterialTheme.typography.body1,
      )

      Text(
          modifier = Modifier.padding(bottom = MaterialTheme.keylines.baseline),
          text = "• Wi-Fi must NOT be connected to any other network",
          style = MaterialTheme.typography.body1,
      )

      Text(
          modifier = Modifier.padding(bottom = MaterialTheme.keylines.baseline),
          text = "• Wi-Fi should be restarted by turning it OFF and then back ON again",
          style = MaterialTheme.typography.body1,
      )

      Text(
          modifier = Modifier.padding(bottom = MaterialTheme.keylines.baseline),
          text = "• The Hotspot password must be at least 8 characters long",
          style = MaterialTheme.typography.body1,
      )

      Text(
          modifier = Modifier.padding(bottom = MaterialTheme.keylines.baseline),
          text = "• The Hotspot name must be unique",
          style = MaterialTheme.typography.body1,
      )
    }

    if (isProxyError) {
      Text(
          modifier = Modifier.padding(bottom = MaterialTheme.keylines.baseline),
          text = "• The Hotspot port number may be already used by a different app",
          style = MaterialTheme.typography.body1,
      )

      Text(
          modifier = Modifier.padding(bottom = MaterialTheme.keylines.baseline),
          text = "• The Hotspot port number must be between 1025 and 65000",
          style = MaterialTheme.typography.body1,
      )
    }
  }
}
