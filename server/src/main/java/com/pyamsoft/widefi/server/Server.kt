package com.pyamsoft.widefi.server

import com.pyamsoft.widefi.server.status.RunningStatus

interface Server {

  suspend fun start()

  suspend fun stop()

  suspend fun onStatusChanged(block: (RunningStatus) -> Unit)

  suspend fun onErrorEvent(block: (ErrorEvent) -> Unit)
}
