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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.IconToggleButton
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.core.text.trimmedLength
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.pydroid.ui.haptics.LocalHapticManager
import com.pyamsoft.tetherfi.server.ServerDefaults
import com.pyamsoft.tetherfi.ui.icons.Visibility
import com.pyamsoft.tetherfi.ui.icons.VisibilityOff

private const val SYSTEM_DEFINED = "SYSTEM DEFINED: CANNOT CHANGE"

@Composable
internal fun EditPort(
    modifier: Modifier = Modifier,
    state: StatusViewState,
    onPortChanged: (String) -> Unit,
) {
  val port by state.port.collectAsStateWithLifecycle()
  val portNumber = remember(port) { "$port" }
  val isValid = remember(port) { port in 1025..64000 }

  StatusEditor(
      modifier = modifier,
      title = "PROXY PORT",
      value = portNumber,
      onChange = onPortChanged,
      keyboardOptions =
          KeyboardOptions(
              keyboardType = KeyboardType.Number,
          ),
      trailingIcon = {
        ValidIcon(
            isValid = isValid,
            what = "Port",
        )
      },
  )
}

@Composable
internal fun EditPassword(
    modifier: Modifier = Modifier,
    state: StatusViewState,
    onPasswordChanged: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
) {
  val canUseCustomConfig = remember { ServerDefaults.canUseCustomConfig() }
  val password by state.password.collectAsStateWithLifecycle()
  val isPasswordVisible by state.isPasswordVisible.collectAsStateWithLifecycle()
  val hotspotPassword =
      remember(
          canUseCustomConfig,
          password,
      ) {
        if (canUseCustomConfig) password else SYSTEM_DEFINED
      }

  val isValid =
      remember(
          canUseCustomConfig,
          password,
      ) {
        if (canUseCustomConfig) password.trimmedLength() in 8..63 else true
      }

  val hapticManager = LocalHapticManager.current

  StatusEditor(
      modifier = modifier,
      enabled = canUseCustomConfig,
      title = "HOTSPOT PASSWORD",
      value = hotspotPassword,
      onChange = onPasswordChanged,
      visualTransformation =
          if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
      keyboardOptions =
          KeyboardOptions(
              keyboardType = KeyboardType.Password,
          ),
      trailingIcon = {
        Row(
            modifier = Modifier.padding(horizontal = MaterialTheme.keylines.content),
            verticalAlignment = Alignment.CenterVertically,
        ) {
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
                contentDescription =
                    if (isPasswordVisible) "Password Visible" else "Password Hidden",
            )
          }

          ValidIcon(
              isValid = isValid,
              what = "Password",
          )
        }
      },
  )
}

@Composable
internal fun EditSsid(
    modifier: Modifier = Modifier,
    state: StatusViewState,
    onSsidChanged: (String) -> Unit,
) {
  val canUseCustomConfig = remember { ServerDefaults.canUseCustomConfig() }
  val ssid by state.ssid.collectAsStateWithLifecycle()
  val hotspotSsid =
      remember(
          canUseCustomConfig,
          ssid,
      ) {
        if (canUseCustomConfig) ssid else SYSTEM_DEFINED
      }

  val isValid =
      remember(
          canUseCustomConfig,
          ssid,
      ) {
        if (canUseCustomConfig) ssid.isNotBlank() else true
      }

  StatusEditor(
      modifier = modifier,
      enabled = canUseCustomConfig,
      title = "HOTSPOT NAME",
      value = hotspotSsid,
      onChange = onSsidChanged,
      leadingIcon = {
        if (canUseCustomConfig) {
          val textStyle = LocalTextStyle.current
          Text(
              modifier =
                  Modifier.padding(
                      start = MaterialTheme.keylines.content,
                      end = MaterialTheme.keylines.typography,
                  ),
              text = remember { ServerDefaults.getSsidPrefix() },
              style =
                  textStyle.copy(
                      fontFamily = FontFamily.Monospace,
                      color =
                          MaterialTheme.colors.onSurface.copy(
                              alpha = ContentAlpha.disabled,
                          ),
                  ),
          )
        }
      },
      trailingIcon = {
        ValidIcon(
            isValid = isValid,
            what = "Name",
        )
      },
  )
}
