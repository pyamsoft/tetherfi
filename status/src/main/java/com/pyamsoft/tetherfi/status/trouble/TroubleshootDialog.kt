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

package com.pyamsoft.tetherfi.status.trouble

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import com.pyamsoft.tetherfi.ui.CardDialog

private enum class TroubleshootDialogContentTypes {
  STEPS
}

@Composable
internal fun TroubleshootDialog(
    modifier: Modifier = Modifier,
    appName: String,
    isBroadcastError: Boolean,
    isProxyError: Boolean,
    onDismiss: () -> Unit,
) {
  val hapticManager = LocalHapticManager.current

  CardDialog(
      modifier = modifier,
      onDismiss = onDismiss,
  ) {
    Column {
      LazyColumn(
          modifier =
              Modifier.weight(
                  weight = 1F,
                  fill = false,
              ),
      ) {
        item(
            contentType = TroubleshootDialogContentTypes.STEPS,
        ) {
          TroubleshootUnableToStart(
              modifier = Modifier.fillMaxWidth().padding(top = MaterialTheme.keylines.content),
              appName = appName,
              isBroadcastError = isBroadcastError,
              isProxyError = isProxyError,
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
              text = stringResource(android.R.string.cancel),
          )
        }
      }
    }
  }
}

@Composable
private fun PreviewTroubleshootDialog(
    isBroadcastError: Boolean,
    isProxyError: Boolean,
) {
  TroubleshootDialog(
      appName = "TEST",
      isBroadcastError = isBroadcastError,
      isProxyError = isProxyError,
      onDismiss = {},
  )
}

@Preview
@Composable
private fun PreviewTroubleshootDialogNone() {
  PreviewTroubleshootDialog(
      isBroadcastError = false,
      isProxyError = false,
  )
}

@Preview
@Composable
private fun PreviewTroubleshootDialogBroadcast() {
  PreviewTroubleshootDialog(
      isBroadcastError = true,
      isProxyError = false,
  )
}

@Preview
@Composable
private fun PreviewTroubleshootDialogProxy() {
  PreviewTroubleshootDialog(
      isBroadcastError = false,
      isProxyError = true,
  )
}

@Preview
@Composable
private fun PreviewTroubleshootDialogBoth() {
  PreviewTroubleshootDialog(
      isBroadcastError = true,
      isProxyError = true,
  )
}
