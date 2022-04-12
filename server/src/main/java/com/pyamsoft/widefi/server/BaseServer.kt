package com.pyamsoft.widefi.server

import com.pyamsoft.widefi.server.status.RunningStatus
import com.pyamsoft.widefi.server.status.StatusBroadcast

internal abstract class BaseServer
protected constructor(
    protected val status: StatusBroadcast,
) : Server {

  init {
    status.set(RunningStatus.NotRunning)
  }

  final override suspend fun onStatusChanged(block: (RunningStatus) -> Unit) {
    return status.onStatus(block)
  }

  final override fun getCurrentStatus(): RunningStatus {
    return status.get()
  }
}
