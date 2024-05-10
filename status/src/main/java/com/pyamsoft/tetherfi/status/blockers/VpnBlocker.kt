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

package com.pyamsoft.tetherfi.status.blockers

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.pydroid.ui.app.rememberDialogProperties
import com.pyamsoft.pydroid.ui.haptics.LocalHapticManager
import com.pyamsoft.tetherfi.status.R

private enum class VpnBlockerContentTypes {
  NOTE,
  RESOLVE,
  PRIVACY_POLICY
}

@Composable
internal fun VpnBlocker(
    modifier: Modifier = Modifier,
    appName: String,
    onDismiss: () -> Unit,
) {
  val hapticManager = LocalHapticManager.current

  Dialog(
      properties = rememberDialogProperties(),
      onDismissRequest = onDismiss,
  ) {
    Card(
        modifier = modifier.padding(MaterialTheme.keylines.content),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.elevatedCardElevation(),
        colors = CardDefaults.elevatedCardColors(),
    ) {
      Text(
          modifier =
              Modifier.padding(horizontal = MaterialTheme.keylines.content)
                  .padding(top = MaterialTheme.keylines.content),
          text = stringResource(R.string.block_vpn_title),
          style = MaterialTheme.typography.headlineSmall,
      )
      LazyColumn(
          modifier =
              Modifier.weight(
                  weight = 1F,
                  fill = false,
              ),
      ) {
        item(
            contentType = VpnBlockerContentTypes.NOTE,
        ) {
          Text(
              modifier =
                  Modifier.padding(horizontal = MaterialTheme.keylines.content)
                      .padding(top = MaterialTheme.keylines.content),
              text = stringResource(R.string.block_vpn_description, appName),
              style = MaterialTheme.typography.bodyLarge,
          )
        }

        item(
            contentType = VpnBlockerContentTypes.RESOLVE,
        ) {
          Text(
              modifier =
                  Modifier.padding(horizontal = MaterialTheme.keylines.content)
                      .padding(top = MaterialTheme.keylines.content),
              text = stringResource(R.string.block_vpn_instruction),
              style = MaterialTheme.typography.bodyLarge,
          )
        }

        item(
            contentType = VpnBlockerContentTypes.PRIVACY_POLICY,
        ) {
          ViewPrivacyPolicy(
              modifier =
                  Modifier.padding(horizontal = MaterialTheme.keylines.content)
                      .padding(top = MaterialTheme.keylines.content),
              appName = appName,
          )
        }
      }

      Row(
          modifier = Modifier.fillMaxWidth().padding(MaterialTheme.keylines.baseline),
          verticalAlignment = Alignment.CenterVertically,
      ) {
        Spacer(
            modifier = Modifier.weight(1F),
        )

        TextButton(
            onClick = {
              hapticManager?.cancelButtonPress()
              onDismiss()
            },
        ) {
          Text(
              text = stringResource(android.R.string.ok),
          )
        }
      }
    }
  }
}

@Preview
@Composable
private fun PreviewVpnBlocker() {
  VpnBlocker(
      appName = "TEST",
      onDismiss = {},
  )
}
