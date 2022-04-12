package com.pyamsoft.widefi.server.status

import androidx.annotation.CheckResult

interface StatusListener {

  suspend fun onStatus(block: (RunningStatus) -> Unit)

  @CheckResult fun get(): RunningStatus
}
