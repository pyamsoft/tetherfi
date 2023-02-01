package com.pyamsoft.tetherfi.info

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.tetherfi.ui.ServerViewState
import com.pyamsoft.tetherfi.ui.TestServerViewState
import com.pyamsoft.tetherfi.ui.icons.QrCode
import com.pyamsoft.tetherfi.ui.icons.Visibility
import com.pyamsoft.tetherfi.ui.icons.VisibilityOff

internal fun LazyListScope.renderDeviceSetup(
    itemModifier: Modifier = Modifier,
    appName: String,
    state: InfoViewState,
    serverViewState: ServerViewState,
    onShowQRCode: () -> Unit,
    onTogglePasswordVisibility: () -> Unit,
) {
  item {
    OtherInstruction(
        modifier = itemModifier,
    ) {
      Text(
          text = "Open the Wi-Fi settings page",
          style = MaterialTheme.typography.body1,
      )
    }
  }

  item {
    OtherInstruction(
        modifier = itemModifier.padding(top = MaterialTheme.keylines.content),
    ) {
      Column {
        Text(
            text = "Connect to the $appName Hotspot",
            style = MaterialTheme.typography.body2,
        )

        Row {
          Text(
              text = "Name",
              style =
                  MaterialTheme.typography.body1.copy(
                      color =
                          MaterialTheme.colors.onBackground.copy(
                              alpha = ContentAlpha.medium,
                          ),
                  ),
          )

          val ssid by serverViewState.ssid.collectAsState()
          Text(
              modifier = Modifier.padding(start = MaterialTheme.keylines.typography),
              text = remember(ssid) { ssid.ifBlank { "NO NAME" } },
              style =
                  MaterialTheme.typography.body1.copy(
                      fontWeight = FontWeight.W700,
                      fontFamily = FontFamily.Monospace,
                  ),
          )

          val password by serverViewState.password.collectAsState()
          val isNetworkReadyForQRCode =
              remember(
                  ssid,
                  password,
              ) {
                ssid.isNotBlank() && password.isNotBlank()
              }

          if (isNetworkReadyForQRCode) {
            // Don't use IconButton because we don't care about minimum touch target size
            Box(
                modifier =
                    Modifier.padding(start = MaterialTheme.keylines.baseline)
                        .clickable { onShowQRCode() }
                        .padding(MaterialTheme.keylines.typography),
                contentAlignment = Alignment.Center,
            ) {
              Icon(
                  modifier = Modifier.size(16.dp),
                  imageVector = Icons.Filled.QrCode,
                  contentDescription = "QR Code",
                  tint = MaterialTheme.colors.primary,
              )
            }
          }
        }

        Row {
          Text(
              text = "Password",
              style =
                  MaterialTheme.typography.body1.copy(
                      color =
                          MaterialTheme.colors.onBackground.copy(
                              alpha = ContentAlpha.medium,
                          ),
                  ),
          )

          val rawPassword by serverViewState.password.collectAsState()
          val isPasswordVisible by state.isPasswordVisible.collectAsState()
          val password =
              remember(
                  rawPassword,
                  isPasswordVisible,
              ) {
                // If hidden password, map each char to the password star
                return@remember if (isPasswordVisible) {
                  rawPassword
                } else {
                  rawPassword.map { '\u2022' }.joinToString("")
                }
              }

          Text(
              modifier = Modifier.padding(start = MaterialTheme.keylines.typography),
              text = remember(password) { password.ifBlank { "NO PASSWORD" } },
              style =
                  MaterialTheme.typography.body1.copy(
                      fontWeight = FontWeight.W700,
                      fontFamily = FontFamily.Monospace,
                  ),
          )

          if (password.isNotBlank()) {
            // Don't use IconButton because we don't care about minimum touch target size
            Box(
                modifier =
                    Modifier.padding(start = MaterialTheme.keylines.baseline)
                        .clickable { onTogglePasswordVisibility() }
                        .padding(MaterialTheme.keylines.typography),
                contentAlignment = Alignment.Center,
            ) {
              Icon(
                  modifier = Modifier.size(16.dp),
                  imageVector =
                      if (isPasswordVisible) Icons.Filled.VisibilityOff
                      else Icons.Filled.Visibility,
                  contentDescription =
                      if (isPasswordVisible) "Password Visible" else "Password Hidden",
                  tint = MaterialTheme.colors.primary,
              )
            }
          }
        }

        Text(
            modifier = Modifier.padding(top = MaterialTheme.keylines.baseline),
            text = "Also configure the Proxy settings",
            style = MaterialTheme.typography.body2,
        )

        Row {
          Text(
              text = "URL",
              style =
                  MaterialTheme.typography.body1.copy(
                      color =
                          MaterialTheme.colors.onBackground.copy(
                              alpha = ContentAlpha.medium,
                          ),
                  ),
          )

          val ip by serverViewState.ip.collectAsState()
          val ipAddress = remember(ip) { ip.ifBlank { "NO IP ADDRESS" } }
          Text(
              modifier = Modifier.padding(start = MaterialTheme.keylines.typography),
              text = ipAddress,
              style =
                  MaterialTheme.typography.body1.copy(
                      fontWeight = FontWeight.W700,
                      fontFamily = FontFamily.Monospace,
                  ),
          )
        }

        Row {
          Text(
              text = "Port",
              style =
                  MaterialTheme.typography.body1.copy(
                      color =
                          MaterialTheme.colors.onBackground.copy(
                              alpha = ContentAlpha.medium,
                          ),
                  ),
          )

          val port by serverViewState.port.collectAsState()
          val portNumber = remember(port) { if (port <= 1024) "INVALID PORT" else "$port" }
          Text(
              modifier = Modifier.padding(start = MaterialTheme.keylines.typography),
              text = portNumber,
              style =
                  MaterialTheme.typography.body1.copy(
                      fontWeight = FontWeight.W700,
                      fontFamily = FontFamily.Monospace,
                  ),
          )
        }

        Text(
            modifier = Modifier.padding(top = MaterialTheme.keylines.typography),
            text = "Leave all other proxy options blank!",
            style =
                MaterialTheme.typography.caption.copy(
                    color =
                        MaterialTheme.colors.onBackground.copy(
                            alpha = ContentAlpha.medium,
                        ),
                ),
        )
      }
    }
  }

  item {
    OtherInstruction(
        modifier = itemModifier.padding(top = MaterialTheme.keylines.content),
    ) {
      Text(
          text =
              "Turn the Wi-Fi off and back on again. It should automatically connect to the $appName Hotspot",
          style = MaterialTheme.typography.body1,
      )
    }
  }
}

@Preview
@Composable
private fun PreviewDeviceSetup() {
  LazyColumn {
    renderDeviceSetup(
        appName = "TEST",
        serverViewState = TestServerViewState(),
        state = MutableInfoViewState(),
        onTogglePasswordVisibility = {},
        onShowQRCode = {},
    )
  }
}
