package com.pyamsoft.widefi.server.status

import kotlinx.coroutines.flow.MutableStateFlow

abstract class BaseStatusBroadcaster protected constructor() : StatusBroadcast {

  private val state = MutableStateFlow<RunningStatus>(RunningStatus.NotRunning)

  final override fun set(status: RunningStatus) {
    val old = state.value
    if (old != status) {
      state.value = status
    }
  }

  final override suspend fun onStatus(block: (RunningStatus) -> Unit) {
    state.collect { block(it) }
  }

  final override fun get(): RunningStatus {
    return state.value
  }
}
