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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.pydroid.ui.app.rememberDialogProperties
import com.pyamsoft.pydroid.ui.defaults.DialogDefaults
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

  Dialog(
      properties = rememberDialogProperties(),
      onDismissRequest = onDismiss,
  ) {
    Box(
        modifier = modifier.padding(MaterialTheme.keylines.content),
    ) {
      Surface(
          modifier = Modifier.fillMaxWidth(),
          shadowElevation = DialogDefaults.Elevation,
          shape = MaterialTheme.shapes.medium,
      ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.keylines.content),
        ) {
          Row(
              verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
                text = "Permission Request",
                style = MaterialTheme.typography.headlineSmall,
            )
          }
          Column {
            Text(
                text = "$appName needs $neededPermission permission to create a Wi-Fi Group",
                style = MaterialTheme.typography.bodyLarge,
            )

            Text(
                modifier = Modifier.padding(top = MaterialTheme.keylines.content),
                text =
                    "$appName will not use these permissions for anything else but Wi-Fi Group creation.",
                style = MaterialTheme.typography.bodyLarge,
            )

            ViewPrivacyPolicy(
                modifier = Modifier.padding(top = MaterialTheme.keylines.content),
            )
          }
          Row(
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
                  text = "Open Settings",
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
        }
      }
    }
  }
}
