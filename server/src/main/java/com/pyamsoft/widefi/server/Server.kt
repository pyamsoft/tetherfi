package com.pyamsoft.widefi.server

import androidx.annotation.CheckResult
import com.pyamsoft.widefi.server.event.ConnectionEvent
import com.pyamsoft.widefi.server.event.ErrorEvent
import com.pyamsoft.widefi.server.status.RunningStatus

interface Server {

  @CheckResult fun getCurrentStatus(): RunningStatus

  suspend fun onStatusChanged(block: (RunningStatus) -> Unit)

  suspend fun onErrorEvent(block: (ErrorEvent) -> Unit)

  suspend fun onConnectionEvent(block: (ConnectionEvent) -> Unit)
}
