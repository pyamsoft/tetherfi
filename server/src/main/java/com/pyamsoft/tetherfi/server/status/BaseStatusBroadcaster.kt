package com.pyamsoft.tetherfi.server.status

import android.content.ComponentName
import android.content.Context
import android.service.quicksettings.TileService
import kotlinx.coroutines.flow.MutableStateFlow
import timber.log.Timber

abstract class BaseStatusBroadcaster
protected constructor(
    private val context: Context,
    private val tileServiceClass: Class<out TileService>,
) : StatusBroadcast {

  private val state = MutableStateFlow<RunningStatus>(RunningStatus.NotRunning)

  private fun requestTileStartListening() {
    Timber.d("Fire broadcast to update TileService")
    TileService.requestListeningState(
        context,
        ComponentName(
            context,
            tileServiceClass,
        ),
    )
  }

  final override fun set(status: RunningStatus) {
    val old = state.value
    if (old != status) {
      state.value = status

      requestTileStartListening()
    }
  }

  final override suspend fun onStatus(block: suspend (RunningStatus) -> Unit) {
    state.collect { block(it) }
  }

  final override fun get(): RunningStatus {
    return state.value
  }
}
