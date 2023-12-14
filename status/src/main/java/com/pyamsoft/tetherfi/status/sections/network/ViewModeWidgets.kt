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

package com.pyamsoft.tetherfi.status.sections.network

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.IconToggleButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.pydroid.ui.haptics.LocalHapticManager
import com.pyamsoft.tetherfi.server.broadcast.BroadcastNetworkStatus
import com.pyamsoft.tetherfi.status.common.StatusItem
import com.pyamsoft.tetherfi.status.StatusViewState
import com.pyamsoft.tetherfi.ui.ServerViewState
import com.pyamsoft.tetherfi.ui.appendLink
import com.pyamsoft.tetherfi.ui.icons.Visibility
import com.pyamsoft.tetherfi.ui.icons.VisibilityOff
import com.pyamsoft.tetherfi.ui.rememberServerHostname
import com.pyamsoft.tetherfi.ui.rememberServerPassword
import com.pyamsoft.tetherfi.ui.rememberServerSSID

private const val SETUP_TAG = "setup_instructions"
private const val SETUP_TEXT = "setup instructions"

@Composable
internal fun ViewProxy(
    modifier: Modifier = Modifier,
    serverViewState: ServerViewState,
) {
  val connection by serverViewState.connection.collectAsStateWithLifecycle()
  val ipAddress = rememberServerHostname(connection)

  val portNumber by serverViewState.port.collectAsStateWithLifecycle()
  val port =
      remember(
          portNumber,
      ) {
        if (portNumber in 1024..65000) "$portNumber" else "INVALID PORT"
      }

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
        value = port,
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
  val group by serverViewState.group.collectAsStateWithLifecycle()
  val isPasswordVisible by state.isPasswordVisible.collectAsStateWithLifecycle()
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

    if (group is BroadcastNetworkStatus.GroupInfo.Connected) {
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
  val group by serverViewState.group.collectAsStateWithLifecycle()
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

@Composable
internal fun ViewInstructions(
    modifier: Modifier = Modifier,
    onJumpToHowTo: () -> Unit,
) {
  val text = buildAnnotatedString {
    appendLine("Confused on what to do next?")
    append("View the ")

    appendLink(
        tag = SETUP_TAG,
        linkColor = MaterialTheme.colors.primary,
        text = SETUP_TEXT,
        url = SETUP_TAG,
    )
  }

  ClickableText(
      modifier = modifier,
      style =
          MaterialTheme.typography.body2.copy(
              color =
                  MaterialTheme.colors.onBackground.copy(
                      alpha = ContentAlpha.medium,
                  ),
              textAlign = TextAlign.Center,
          ),
      text = text,
      onClick = { start ->
        text
            .getStringAnnotations(
                tag = SETUP_TAG,
                start = start,
                end = start + SETUP_TAG.length,
            )
            .firstOrNull()
            ?.also { onJumpToHowTo() }
      },
  )
}
