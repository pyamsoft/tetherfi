package com.pyamsoft.tetherfi.info

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
      Text(
          text = "Connect to the Internet.",
          style = MaterialTheme.typography.body1,
      )
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
      Text(
          text = "Start the $appName Hotspot.",
          style = MaterialTheme.typography.body1,
      )
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
