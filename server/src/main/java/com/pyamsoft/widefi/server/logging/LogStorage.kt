package com.pyamsoft.widefi.server.logging

import com.pyamsoft.widefi.core.LogEvent

interface LogStorage<T : LogEvent> {

  suspend fun onLogEvent(block: suspend (T) -> Unit)

  suspend fun submit(event: T)

  suspend fun clear()
}
