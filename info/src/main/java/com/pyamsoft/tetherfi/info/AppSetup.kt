package com.pyamsoft.tetherfi.info

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.tetherfi.server.ServerDefaults

internal fun LazyListScope.renderAppSetup(
    itemModifier: Modifier = Modifier,
    appName: String,
) {
  item {
    ThisInstruction(
        modifier = itemModifier,
    ) {
      Column {
        Text(
            text = "Turn on Wi-Fi.",
            style = MaterialTheme.typography.body1,
        )
        Text(
            text =
                "You do not need to connect to a Network, but Wi-Fi must be on for $appName to work.",
            style =
                MaterialTheme.typography.body2.copy(
                    color =
                        MaterialTheme.colors.onSurface.copy(
                            alpha = ContentAlpha.medium,
                        ),
                ),
        )
      }
    }
  }

  item {
    ThisInstruction(
        modifier = itemModifier.padding(top = MaterialTheme.keylines.content),
    ) {
      Column {
        Text(
            text = "Connect to the Internet.",
            style = MaterialTheme.typography.body1,
        )
        Text(
            text = "You can connect via Wi-Fi or Mobile Data",
            style =
                MaterialTheme.typography.body2.copy(
                    color =
                        MaterialTheme.colors.onSurface.copy(
                            alpha = ContentAlpha.medium,
                        ),
                ),
        )
      }
    }
  }

  item {
    val canConfigure = remember { ServerDefaults.canUseCustomConfig() }
    if (canConfigure) {
      ThisInstruction(
          modifier = itemModifier.padding(top = MaterialTheme.keylines.content),
          small = true,
      ) {
        Text(
            text =
                "You can optionally configure the Name/SSID, Password, Proxy Port and Broadcast Frequency Band. This is not required, and the defaults will work fine for most people.",
            style =
                MaterialTheme.typography.body2.copy(
                    color =
                        MaterialTheme.colors.onSurface.copy(
                            alpha = ContentAlpha.medium,
                        ),
                ),
        )
      }
    }
  }

  item {
    ThisInstruction(
        modifier = itemModifier.padding(top = MaterialTheme.keylines.content),
        small = true,
    ) {
      Text(
          text =
              "You can optionally choose to keep the CPU awake while the Hotspot is running, which greatly improves performance while the screen is off. If the CPU is not kept awake, you may notice extreme network slowdown while the device screen is off.",
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

  item {
    ThisInstruction(
        modifier = itemModifier.padding(top = MaterialTheme.keylines.content),
        small = true,
    ) {
      Text(
          text =
              "You can optionally choose to ignore Android's system battery optimizations, which will allow $appName to run at maximum performance at all times. This may use more battery but will significantly enhance your networking experience.",
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

  item {
    ThisInstruction(
        modifier = itemModifier.padding(top = MaterialTheme.keylines.content),
    ) {
      Column {
        Text(
            text = "Start the $appName Hotspot.",
            style = MaterialTheme.typography.body1,
        )
        Text(
            text = "Check that the Broadcast, Proxy, and Hotspot status are all green and running.",
            style =
                MaterialTheme.typography.body2.copy(
                    color =
                        MaterialTheme.colors.onSurface.copy(
                            alpha = ContentAlpha.medium,
                        ),
                ),
        )
      }
    }
  }
}

@Preview
@Composable
private fun PreviewAppSetup() {
  LazyColumn {
    renderAppSetup(
        appName = "TEST",
    )
  }
}
