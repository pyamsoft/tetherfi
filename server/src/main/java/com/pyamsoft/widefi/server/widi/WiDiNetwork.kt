package com.pyamsoft.widefi.server.widi

import androidx.annotation.CheckResult
import com.pyamsoft.widefi.server.Server
import com.pyamsoft.widefi.server.ServerNetworkBand
import com.pyamsoft.widefi.server.status.RunningStatus
import com.pyamsoft.widefi.server.widi.receiver.WidiNetworkEvent

interface WiDiNetwork : Server {

  suspend fun start(onStart: () -> Unit)

  suspend fun stop(onStop: () -> Unit)

  @CheckResult suspend fun getGroupInfo(): GroupInfo?

  @CheckResult suspend fun getConnectionInfo(): ConnectionInfo?

  suspend fun onProxyStatusChanged(block: (RunningStatus) -> Unit)

  suspend fun onWifiDirectEvent(block: (WidiNetworkEvent) -> Unit)

  data class GroupInfo
  internal constructor(
      val ssid: String,
      val password: String,
      val band: ServerNetworkBand,
  )

  data class ConnectionInfo
  internal constructor(
      val ip: String,
      val hostName: String,
  )
}
