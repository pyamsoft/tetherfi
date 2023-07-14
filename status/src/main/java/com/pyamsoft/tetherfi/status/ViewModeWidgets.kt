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

package com.pyamsoft.tetherfi.status

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.IconToggleButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.pydroid.ui.haptics.LocalHapticManager
import com.pyamsoft.tetherfi.server.widi.WiDiNetworkStatus
import com.pyamsoft.tetherfi.ui.ServerViewState
import com.pyamsoft.tetherfi.ui.icons.Visibility
import com.pyamsoft.tetherfi.ui.icons.VisibilityOff
import com.pyamsoft.tetherfi.ui.rememberServerHostname
import com.pyamsoft.tetherfi.ui.rememberServerPassword
import com.pyamsoft.tetherfi.ui.rememberServerSSID

@Composable
internal fun ViewProxy(
    modifier: Modifier = Modifier,
    serverViewState: ServerViewState,
) {
  val connection by serverViewState.connection.collectAsState()
  val ipAddress = rememberServerHostname(connection)

  val port by serverViewState.port.collectAsState()
  val portNumber = remember(port) { if (port <= 1024) "INVALID PORT" else "$port" }

  Row(
      modifier = modifier,
      verticalAlignment = Alignment.CenterVertically,
  ) {
    StatusItem(
        modifier = Modifier.weight(1F, fill = false),
        title = "PROXY URL/HOSTNAME",
        value = ipAddress,
        valueStyle =
            MaterialTheme.typography.h6.copy(
                fontWeight = FontWeight.W400,
                fontFamily = FontFamily.Monospace,
            ),
    )

    Spacer(
        modifier = Modifier.width(MaterialTheme.keylines.content),
    )

    StatusItem(
        title = "PROXY PORT",
        value = portNumber,
        valueStyle =
            MaterialTheme.typography.h6.copy(
                fontWeight = FontWeight.W400,
                fontFamily = FontFamily.Monospace,
            ),
    )
  }
}

@Composable
internal fun ViewPassword(
    modifier: Modifier = Modifier,
    state: StatusViewState,
    serverViewState: ServerViewState,
    onTogglePasswordVisibility: () -> Unit,
) {
  val hapticManager = LocalHapticManager.current
  val group by serverViewState.group.collectAsState()
  val isPasswordVisible by state.isPasswordVisible.collectAsState()
  val password = rememberServerPassword(group, isPasswordVisible)

  Row(
      modifier = modifier,
      verticalAlignment = Alignment.CenterVertically,
  ) {
    StatusItem(
        modifier = Modifier.padding(end = MaterialTheme.keylines.content),
        title = "HOTSPOT PASSWORD",
        value = password,
        valueStyle =
            MaterialTheme.typography.h6.copy(
                fontWeight = FontWeight.W400,
                fontFamily = FontFamily.Monospace,
            ),
    )

    if (group is WiDiNetworkStatus.GroupInfo.Connected) {
      IconToggleButton(
          checked = isPasswordVisible,
          onCheckedChange = { newVisible ->
            if (newVisible) {
              hapticManager?.toggleOn()
            } else {
              hapticManager?.toggleOff()
            }
            onTogglePasswordVisibility()
          },
      ) {
        Icon(
            imageVector =
                if (isPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
            contentDescription = if (isPasswordVisible) "Password Visible" else "Password Hidden",
            tint = MaterialTheme.colors.primary,
        )
      }
    }
  }
}

@Composable
internal fun ViewSsid(
    modifier: Modifier = Modifier,
    serverViewState: ServerViewState,
) {
  val group by serverViewState.group.collectAsState()
  val ssid = rememberServerSSID(group)

  Row(
      modifier = modifier,
      verticalAlignment = Alignment.CenterVertically,
  ) {
    StatusItem(
        modifier = Modifier.padding(end = MaterialTheme.keylines.content),
        title = "HOTSPOT NAME",
        value = ssid,
        valueStyle =
            MaterialTheme.typography.h6.copy(
                fontWeight = FontWeight.W400,
                fontFamily = FontFamily.Monospace,
            ),
    )
  }
}
