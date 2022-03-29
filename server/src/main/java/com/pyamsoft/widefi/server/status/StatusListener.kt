package com.pyamsoft.widefi.server.status

interface StatusListener {

  suspend fun onStatus(block: (RunningStatus) -> Unit)
}
