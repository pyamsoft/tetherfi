package com.pyamsoft.tetherfi.server.status

interface StatusBroadcast : StatusListener {

  fun set(status: RunningStatus)
}
