package com.pyamsoft.tetherfi.service.lock

interface Locker {

  suspend fun acquire()

  suspend fun release()
}
