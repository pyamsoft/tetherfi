package com.pyamsoft.tetherfi.server.status

sealed class RunningStatus {
  object NotRunning : RunningStatus()
  object Starting : RunningStatus()
  object Running : RunningStatus()
  object Stopping : RunningStatus()
  data class Error (val message: String) : RunningStatus()
}
