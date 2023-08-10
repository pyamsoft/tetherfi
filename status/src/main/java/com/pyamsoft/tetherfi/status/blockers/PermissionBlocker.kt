/*
 * Copyright 2023 pyamsoft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.pydroid.ui.app.rememberDialogProperties
import com.pyamsoft.pydroid.ui.haptics.LocalHapticManager

@Composable
internal fun PermissionBlocker(
    modifier: Modifier = Modifier,
    appName: String,
    onDismiss: () -> Unit,
    onOpenPermissionSettings: () -> Unit,
    onRequestPermissions: () -> Unit,
) {
  val hapticManager = LocalHapticManager.current

  // Permission needed is different on T
  val neededPermission = remember {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
      "PRECISE LOCATION"
    } else {
      "NEARBY WIFI"
    }
  }

  AlertDialog(
      modifier = modifier.padding(MaterialTheme.keylines.content),
      properties = rememberDialogProperties(),
      onDismissRequest = onDismiss,
      title = {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
              text = "Permission Request",
              style = MaterialTheme.typography.h5,
          )
        }
      },
      text = {
        Column {
          Text(
              text = "$appName needs $neededPermission permission to create a Wi-Fi Group",
              style = MaterialTheme.typography.body1,
          )

          Text(
              modifier = Modifier.padding(top = MaterialTheme.keylines.content),
              text =
                  "$appName will not use these permissions for anything else but Wi-Fi Group creation.",
              style = MaterialTheme.typography.body1,
          )

          ViewPrivacyPolicy(
              modifier = Modifier.padding(top = MaterialTheme.keylines.content),
          )
        }
      },
      buttons = {
        Row(
            modifier =
                Modifier.padding(horizontal = MaterialTheme.keylines.content)
                    .padding(bottom = MaterialTheme.keylines.baseline),
            verticalAlignment = Alignment.CenterVertically,
        ) {
          TextButton(
              onClick = {
                hapticManager?.actionButtonPress()
                onOpenPermissionSettings()
              },
              colors =
                  ButtonDefaults.textButtonColors(
                      contentColor =
                          MaterialTheme.colors.onSurface.copy(
                              alpha = ContentAlpha.medium,
                          ),
                  ),
          ) {
            Text(
                text = "Open Settings",
            )
          }

          Spacer(
              modifier = Modifier.weight(1F),
          )

          TextButton(
              colors =
                  ButtonDefaults.textButtonColors(
                      contentColor =
                          MaterialTheme.colors.error.copy(
                              alpha = ContentAlpha.medium,
                          ),
                  ),
              onClick = {
                hapticManager?.cancelButtonPress()
                onDismiss()
              },
          ) {
            Text(
                text = "Deny",
            )
          }

          TextButton(
              onClick = {
                hapticManager?.confirmButtonPress()
                onRequestPermissions()
              },
          ) {
            Text(
                text = "Grant",
            )
          }
        }
      },
  )
}

