/*
 * Copyright 2025 pyamsoft
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

package com.pyamsoft.tetherfi.main.blockers

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.pydroid.ui.haptics.LocalHapticManager
import com.pyamsoft.tetherfi.main.R
import com.pyamsoft.tetherfi.ui.R as R2
import com.pyamsoft.tetherfi.ui.dialog.CardDialog

private enum class LocationBlockerContentTypes {
  NOTE,
  RESOLVE,
  OPEN_SETTINGS,
  PRIVACY_POLICY,
}

@Composable
internal fun LocationBlocker(
    modifier: Modifier = Modifier,
    appName: String,
    onOpenLocationSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
  val hapticManager = LocalHapticManager.current

  CardDialog(
      modifier = modifier,
      onDismiss = onDismiss,
  ) {
    Text(
        modifier =
            Modifier.padding(horizontal = MaterialTheme.keylines.content)
                .padding(top = MaterialTheme.keylines.content),
        text = stringResource(R.string.block_location_title),
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
          contentType = LocationBlockerContentTypes.NOTE,
      ) {
        Text(
            modifier =
                Modifier.padding(horizontal = MaterialTheme.keylines.content)
                    .padding(top = MaterialTheme.keylines.content),
            text = stringResource(R.string.block_location_description, appName),
            style = MaterialTheme.typography.bodyLarge,
        )
      }

      item(
          contentType = LocationBlockerContentTypes.RESOLVE,
      ) {
        val tweak = stringResource(R2.string.ignore_location_title)
        Text(
            modifier =
                Modifier.padding(horizontal = MaterialTheme.keylines.content)
                    .padding(top = MaterialTheme.keylines.content),
            text = stringResource(R.string.block_location_instruction, tweak, appName),
            style = MaterialTheme.typography.bodyLarge,
        )
      }

      item(
          contentType = LocationBlockerContentTypes.OPEN_SETTINGS,
      ) {
        Box(
            modifier =
                Modifier.padding(horizontal = MaterialTheme.keylines.content)
                    .padding(top = MaterialTheme.keylines.content)
                    .fillParentMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
          Button(
              onClick = {
                hapticManager?.confirmButtonPress()
                onOpenLocationSettings()
              },
          ) {
            Text(
                text = stringResource(R.string.block_location_open_settings),
            )
          }
        }
      }

      item(
          contentType = LocationBlockerContentTypes.PRIVACY_POLICY,
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

@Preview
@Composable
private fun PreviewLocationBlocker() {
  LocationBlocker(
      appName = "TEST",
      onDismiss = {},
      onOpenLocationSettings = {},
  )
}
