package com.pyamsoft.widefi.server.widi

import androidx.annotation.CheckResult
import com.pyamsoft.widefi.server.Server
import com.pyamsoft.widefi.server.status.RunningStatus

interface WiDiNetwork : Server {

    suspend fun start()

    suspend fun stop()

  @CheckResult suspend fun getGroupInfo(): GroupInfo?

  suspend fun onProxyStatusChanged(block: (RunningStatus) -> Unit)

  data class GroupInfo
  internal constructor(
      val ssid: String,
      val password: String,
  )
}
