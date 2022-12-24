package com.pyamsoft.tetherfi.info

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.pyamsoft.pydroid.theme.keylines

internal fun LazyListScope.renderDeviceSetup(
    itemModifier: Modifier = Modifier,
    appName: String,
    state: InfoViewState,
) {
  item {
    OtherInstruction(
        modifier = itemModifier,
    ) {
      Text(
          text = "Open the Wi-Fi settings page",
          style = MaterialTheme.typography.body1,
      )
    }
  }

  item {
    OtherInstruction(
        modifier = itemModifier.padding(top = MaterialTheme.keylines.content),
    ) {
      Column {
        Text(
            text = "Connect to the $appName Hotspot",
            style = MaterialTheme.typography.body2,
        )

        Row {
          Text(
              text = "Name/SSID",
              style =
                  MaterialTheme.typography.body1.copy(
                      color =
                          MaterialTheme.colors.onBackground.copy(
                              alpha = ContentAlpha.medium,
                          ),
                  ),
          )
          Text(
              modifier = Modifier.padding(start = MaterialTheme.keylines.typography),
              text = state.ssid,
              style =
                  MaterialTheme.typography.body1.copy(
                      fontWeight = FontWeight.W700,
                  ),
          )
        }

        Row {
          Text(
              text = "Password",
              style =
                  MaterialTheme.typography.body1.copy(
                      color =
                          MaterialTheme.colors.onBackground.copy(
                              alpha = ContentAlpha.medium,
                          ),
                  ),
          )
          Text(
              modifier = Modifier.padding(start = MaterialTheme.keylines.typography),
              text = state.password,
              style =
                  MaterialTheme.typography.body1.copy(
                      fontWeight = FontWeight.W700,
                  ),
          )
        }

        Text(
            modifier = Modifier.padding(top = MaterialTheme.keylines.baseline),
            text = "Also configure the Proxy settings",
            style = MaterialTheme.typography.body2,
        )

        Row {
          Text(
              text = "Proxy URL/Hostname",
              style =
                  MaterialTheme.typography.body1.copy(
                      color =
                          MaterialTheme.colors.onBackground.copy(
                              alpha = ContentAlpha.medium,
                          ),
                  ),
          )
          Text(
              modifier = Modifier.padding(start = MaterialTheme.keylines.typography),
              text = state.ip,
              style =
                  MaterialTheme.typography.body1.copy(
                      fontWeight = FontWeight.W700,
                  ),
          )
        }

        Row {
          Text(
              text = "Proxy Port",
              style =
                  MaterialTheme.typography.body1.copy(
                      color =
                          MaterialTheme.colors.onBackground.copy(
                              alpha = ContentAlpha.medium,
                          ),
                  ),
          )
          Text(
              modifier = Modifier.padding(start = MaterialTheme.keylines.typography),
              text = "${state.port}",
              style =
                  MaterialTheme.typography.body1.copy(
                      fontWeight = FontWeight.W700,
                  ),
          )
        }

        Text(
            text = "Leave all other proxy options blank!",
            style =
                MaterialTheme.typography.caption.copy(
                    color =
                        MaterialTheme.colors.onBackground.copy(
                            alpha = ContentAlpha.medium,
                        ),
                ),
        )
      }
    }
  }

  item {
    OtherInstruction(
        modifier = itemModifier.padding(top = MaterialTheme.keylines.content),
    ) {
      Text(
          text =
              "Turn the Wi-Fi off and back on again. It should automatically connect to the $appName Hotspot",
          style = MaterialTheme.typography.body1,
      )
    }
  }
}

@Preview
@Composable
private fun PreviewDeviceSetup() {
  LazyColumn {
    renderDeviceSetup(
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
}
