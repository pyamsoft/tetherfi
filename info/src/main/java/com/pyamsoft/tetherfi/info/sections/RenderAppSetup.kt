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

package com.pyamsoft.tetherfi.info.sections

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.pydroid.ui.defaults.TypographyDefaults
import com.pyamsoft.tetherfi.info.R
import com.pyamsoft.tetherfi.server.ServerDefaults

private enum class AppSetupContentTypes {
  WIFI,
  INTERNET,
  CONFIG,
  WAKELOCK,
  BATTERY,
  START,
}

internal fun LazyListScope.renderAppSetup(
    itemModifier: Modifier = Modifier,
    appName: String,
) {
  item(
      contentType = AppSetupContentTypes.WIFI,
  ) {
    ThisInstruction(
        modifier = itemModifier,
    ) {
      Column {
        Text(
            text = stringResource(R.string.turn_on_wi_fi),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = stringResource(R.string.wifi_must_be_on, appName),
            style =
                MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
        )
      }
    }
  }

  item(
      contentType = AppSetupContentTypes.INTERNET,
  ) {
    ThisInstruction(
        modifier = itemModifier.padding(top = MaterialTheme.keylines.content),
    ) {
      Column {
        Text(
            text = stringResource(R.string.connect_internet),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = stringResource(R.string.connect_internet_options),
            style =
                MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
        )
      }
    }
  }

  item(
      contentType = AppSetupContentTypes.CONFIG,
  ) {
    if (ServerDefaults.canUseCustomConfig()) {
      ThisInstruction(
          modifier = itemModifier.padding(top = MaterialTheme.keylines.content),
          small = true,
      ) {
        Text(
            text = stringResource(R.string.optionally_configure_hotspot),
            style =
                MaterialTheme.typography.bodyMedium.copy(
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = TypographyDefaults.ALPHA_DISABLED,
                        ),
                ),
        )
      }
    }
  }

  item(
      contentType = AppSetupContentTypes.WAKELOCK,
  ) {
    ThisInstruction(
        modifier = itemModifier.padding(top = MaterialTheme.keylines.content),
        small = true,
    ) {
      Text(
          text = stringResource(R.string.optionally_configure_wakelock),
          style =
              MaterialTheme.typography.bodyMedium.copy(
                  color =
                      MaterialTheme.colorScheme.onSurfaceVariant.copy(
                          alpha = TypographyDefaults.ALPHA_DISABLED,
                      ),
              ),
      )
    }
  }

  item(
      contentType = AppSetupContentTypes.BATTERY,
  ) {
    ThisInstruction(
        modifier = itemModifier.padding(top = MaterialTheme.keylines.content),
        small = true,
    ) {
      Text(
          text = stringResource(R.string.optionally_configure_power, appName),
          style =
              MaterialTheme.typography.bodyMedium.copy(
                  color =
                      MaterialTheme.colorScheme.onSurfaceVariant.copy(
                          alpha = TypographyDefaults.ALPHA_DISABLED,
                      ),
              ),
      )
    }
  }

  item(
      contentType = AppSetupContentTypes.START,
  ) {
    ThisInstruction(
        modifier = itemModifier.padding(top = MaterialTheme.keylines.content),
    ) {
      Column {
        Text(
            text = stringResource(R.string.start_the_hotspot, appName),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = stringResource(R.string.check_hotspot_green),
            style =
                MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
        )
      }
    }
  }
}

@Composable
@Preview(showBackground = true)
private fun PreviewAppSetup() {
  LazyColumn {
    renderAppSetup(
        appName = "TEST",
    )
  }
}
