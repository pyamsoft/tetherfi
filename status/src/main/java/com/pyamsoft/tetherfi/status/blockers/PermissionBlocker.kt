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

import android.os.Build
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.pydroid.ui.haptics.LocalHapticManager
import com.pyamsoft.tetherfi.status.R
import com.pyamsoft.tetherfi.ui.CardDialog

private enum class PermissionBlockerContentTypes {
  EXPLAIN,
  REASSURE,
  PRIVACY_POLICY,
}

@Composable
internal fun PermissionBlocker(
    modifier: Modifier = Modifier,
    appName: String,
    onDismiss: () -> Unit,
    onOpenPermissionSettings: () -> Unit,
    onRequestPermissions: () -> Unit,
) {
  val context = LocalContext.current
  val hapticManager = LocalHapticManager.current

  // Permission needed is different on T
  val neededPermission =
      remember(context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
          context.getString(R.string.permission_type_location)
        } else {
          context.getString(R.string.permission_type_nearby)
        }
      }

  CardDialog(
      modifier = modifier,
      onDismiss = onDismiss,
  ) {
    Text(
        modifier =
            Modifier.padding(horizontal = MaterialTheme.keylines.content)
                .padding(top = MaterialTheme.keylines.content),
        text = stringResource(R.string.permission_title),
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
          contentType = PermissionBlockerContentTypes.EXPLAIN,
      ) {
        Text(
            modifier =
                Modifier.padding(horizontal = MaterialTheme.keylines.content)
                    .padding(top = MaterialTheme.keylines.content),
            text = stringResource(R.string.permission_description, appName, neededPermission),
            style = MaterialTheme.typography.bodyLarge,
        )
      }

      item(
          contentType = PermissionBlockerContentTypes.REASSURE,
      ) {
        Text(
            modifier =
                Modifier.padding(horizontal = MaterialTheme.keylines.content)
                    .padding(top = MaterialTheme.keylines.content),
            text = stringResource(R.string.permission_reassure, appName),
            style = MaterialTheme.typography.bodyLarge,
        )
      }

      item(
          contentType = PermissionBlockerContentTypes.PRIVACY_POLICY,
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
      TextButton(
          onClick = {
            hapticManager?.actionButtonPress()
            onOpenPermissionSettings()
          },
          colors =
              ButtonDefaults.textButtonColors(
                  contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
              ),
      ) {
        Text(
            text = stringResource(R.string.permission_open_settings),
        )
      }

      Spacer(
          modifier = Modifier.weight(1F),
      )

      TextButton(
          colors =
              ButtonDefaults.textButtonColors(
                  contentColor = MaterialTheme.colorScheme.error,
              ),
          onClick = {
            hapticManager?.cancelButtonPress()
            onDismiss()
          },
      ) {
        Text(
            text = stringResource(R.string.permission_deny),
        )
      }

      TextButton(
          onClick = {
            hapticManager?.confirmButtonPress()
            onRequestPermissions()
          },
      ) {
        Text(
            text = stringResource(R.string.permission_grant),
        )
      }
    }
  }
}

@Preview
@Composable
private fun PreviewPermissionBlocker() {
  PermissionBlocker(
      appName = "TEST",
      onDismiss = {},
      onOpenPermissionSettings = {},
      onRequestPermissions = {},
  )
}
