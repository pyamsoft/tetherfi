package com.pyamsoft.tetherfi.server.lock

interface Locker {

  suspend fun acquire()

  suspend fun release()
}
