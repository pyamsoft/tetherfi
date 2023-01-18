package com.pyamsoft.tetherfi.server.status

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

@Stable
@Immutable
sealed class RunningStatus {
  object NotRunning : RunningStatus()
  object Starting : RunningStatus()
  object Running : RunningStatus()
  object Stopping : RunningStatus()
  data class Error(val message: String) : RunningStatus()
}
