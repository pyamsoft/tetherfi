package com.pyamsoft.tetherfi.server

import androidx.annotation.CheckResult
import com.pyamsoft.tetherfi.server.event.ConnectionEvent
import com.pyamsoft.tetherfi.server.event.ErrorEvent
import com.pyamsoft.tetherfi.server.status.RunningStatus

interface Server {

  @CheckResult fun getCurrentStatus(): RunningStatus

  suspend fun onStatusChanged(block: suspend (RunningStatus) -> Unit)

  suspend fun onErrorEvent(block: suspend (ErrorEvent) -> Unit)

  suspend fun onConnectionEvent(block: suspend (ConnectionEvent) -> Unit)
}
