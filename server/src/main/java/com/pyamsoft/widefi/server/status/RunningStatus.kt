package com.pyamsoft.widefi.server.status

sealed class RunningStatus {
  object NotRunning : RunningStatus()
  object Starting : RunningStatus()
  object Running : RunningStatus()
  object Stopping : RunningStatus()
  data class Error internal constructor(val message: String) : RunningStatus()
}
