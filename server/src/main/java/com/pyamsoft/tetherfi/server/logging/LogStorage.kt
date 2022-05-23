package com.pyamsoft.tetherfi.server.logging

import com.pyamsoft.tetherfi.core.LogEvent

interface LogStorage<T : LogEvent> {

  suspend fun onLogEvent(block: suspend (T) -> Unit)

  suspend fun submit(event: T)

  suspend fun clear()
}
