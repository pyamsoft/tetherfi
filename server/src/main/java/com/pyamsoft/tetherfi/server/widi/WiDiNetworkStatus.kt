package com.pyamsoft.tetherfi.server.widi

import androidx.annotation.CheckResult
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.pyamsoft.tetherfi.server.Server
import com.pyamsoft.tetherfi.server.status.RunningStatus

interface WiDiNetworkStatus : Server {

  @CheckResult suspend fun getGroupInfo(): GroupInfo?

  @CheckResult suspend fun getConnectionInfo(): ConnectionInfo?

  suspend fun onProxyStatusChanged(block: suspend (RunningStatus) -> Unit)

  @Stable
  @Immutable
  data class GroupInfo
  internal constructor(
      val ssid: String,
      val password: String,
  )

  @Stable
  @Immutable
  data class ConnectionInfo
  internal constructor(
      val ip: String,
      val hostName: String,
  )
}
