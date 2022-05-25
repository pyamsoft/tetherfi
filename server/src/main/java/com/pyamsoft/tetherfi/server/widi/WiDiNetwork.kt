package com.pyamsoft.tetherfi.server.widi

import androidx.annotation.CheckResult
import com.pyamsoft.tetherfi.server.Server
import com.pyamsoft.tetherfi.server.status.RunningStatus
import com.pyamsoft.tetherfi.server.widi.receiver.WidiNetworkEvent

interface WiDiNetwork : Server {

  fun start(onStart: () -> Unit)

  fun stop(onStop: () -> Unit)

  @CheckResult suspend fun getGroupInfo(): GroupInfo?

  @CheckResult suspend fun getConnectionInfo(): ConnectionInfo?

  suspend fun onProxyStatusChanged(block: suspend (RunningStatus) -> Unit)

  suspend fun onWifiDirectEvent(block: suspend (WidiNetworkEvent) -> Unit)

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
