package com.pyamsoft.tetherfi.server.widi

import androidx.annotation.CheckResult
import com.pyamsoft.tetherfi.server.Server
import com.pyamsoft.tetherfi.server.status.RunningStatus

interface WiDiNetworkStatus : Server {

  @CheckResult suspend fun getGroupInfo(): GroupInfo?

  @CheckResult suspend fun getConnectionInfo(): ConnectionInfo?

  suspend fun onProxyStatusChanged(block: suspend (RunningStatus) -> Unit)

  data class GroupInfo
  internal constructor(
      val ssid: String,
      val password: String,
  )

  data class ConnectionInfo
  internal constructor(
      val ip: String,
      val hostName: String,
  )
}
