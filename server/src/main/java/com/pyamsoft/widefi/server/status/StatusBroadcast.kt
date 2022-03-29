package com.pyamsoft.widefi.server.status

interface StatusBroadcast : StatusListener {

  fun set(status: RunningStatus)
}
