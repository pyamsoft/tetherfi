package com.pyamsoft.widefi.server.status

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect

abstract class BaseStatusBroadcaster protected constructor() : StatusBroadcast {

  private val state = MutableStateFlow<RunningStatus>(RunningStatus.NotRunning)

  override fun set(status: RunningStatus) {
    val old = state.value
    if (old != status) {
      state.value = status
    }
  }

  override suspend fun onStatus(block: (RunningStatus) -> Unit) {
    return state.collect { block(it) }
  }
}
