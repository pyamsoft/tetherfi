package com.pyamsoft.tetherfi.server

import com.pyamsoft.tetherfi.server.status.RunningStatus
import com.pyamsoft.tetherfi.server.status.StatusBroadcast

internal abstract class BaseServer
protected constructor(
    protected val status: StatusBroadcast,
) : Server {

  init {
    status.set(RunningStatus.NotRunning)
  }

  final override suspend fun onStatusChanged(block: suspend (RunningStatus) -> Unit) {
    return status.onStatus(block)
  }

  final override fun getCurrentStatus(): RunningStatus {
    return status.get()
  }
}
