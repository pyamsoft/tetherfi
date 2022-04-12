package com.pyamsoft.widefi.server.widi

import androidx.annotation.CheckResult
import com.pyamsoft.widefi.server.Server
import com.pyamsoft.widefi.server.status.RunningStatus

interface WiDiNetwork : Server {

  suspend fun start(onStart: () -> Unit)

  suspend fun stop(onStop: () -> Unit)

  @CheckResult suspend fun getGroupInfo(): GroupInfo?

  @CheckResult suspend fun getConnectionInfo(): ConnectionInfo?

  suspend fun onProxyStatusChanged(block: (RunningStatus) -> Unit)

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
