package com.pyamsoft.tetherfi.ui

import androidx.annotation.CheckResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.pyamsoft.tetherfi.server.widi.WiDiNetworkStatus

@Composable
@CheckResult
fun rememberServerSSID(group: WiDiNetworkStatus.GroupInfo): String {
  return remember(group) {
    when (group) {
      is WiDiNetworkStatus.GroupInfo.Connected -> group.ssid
      is WiDiNetworkStatus.GroupInfo.Empty -> "NO NAME"
      is WiDiNetworkStatus.GroupInfo.Error -> "ERROR"
    }
  }
}

@Composable
@CheckResult
fun rememberServerRawPassword(group: WiDiNetworkStatus.GroupInfo): String {
  return remember(
      group,
  ) {
    when (group) {
      is WiDiNetworkStatus.GroupInfo.Connected -> group.password
      is WiDiNetworkStatus.GroupInfo.Empty -> ""
      is WiDiNetworkStatus.GroupInfo.Error -> ""
    }
  }
}

@Composable
@CheckResult
fun rememberServerPassword(
    group: WiDiNetworkStatus.GroupInfo,
    isPasswordVisible: Boolean,
): String {
  return remember(
      group,
      isPasswordVisible,
  ) {
    when (group) {
      is WiDiNetworkStatus.GroupInfo.Connected -> {
        val rawPassword = group.password
        // If hidden password, map each char to the password star
        return@remember if (isPasswordVisible) {
          rawPassword
        } else {
          rawPassword.map { '\u2022' }.joinToString("")
        }
      }
      is WiDiNetworkStatus.GroupInfo.Empty -> {
        "NO PASSWORD"
      }
      is WiDiNetworkStatus.GroupInfo.Error -> {
        "ERROR"
      }
    }
  }
}

@Composable
@CheckResult
fun rememberServerIp(connection: WiDiNetworkStatus.ConnectionInfo): String {
  return remember(connection) {
    when (connection) {
      is WiDiNetworkStatus.ConnectionInfo.Connected -> connection.ip
      is WiDiNetworkStatus.ConnectionInfo.Empty -> "NO IP"
      is WiDiNetworkStatus.ConnectionInfo.Error -> "ERROR"
    }
  }
}
