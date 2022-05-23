package com.pyamsoft.tetherfi.server.status

import androidx.annotation.CheckResult

interface StatusListener {

  suspend fun onStatus(block: suspend (RunningStatus) -> Unit)

  @CheckResult fun get(): RunningStatus
}
