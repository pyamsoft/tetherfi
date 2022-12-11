package com.pyamsoft.tetherfi.server

import androidx.annotation.CheckResult
import com.pyamsoft.tetherfi.server.status.RunningStatus

interface Server {

  @CheckResult fun getCurrentStatus(): RunningStatus

  suspend fun onStatusChanged(block: suspend (RunningStatus) -> Unit)
}
