package com.pyamsoft.tetherfi.tile

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Singleton
internal class DefaultTileStatus @Inject internal constructor() : TileStatus {

  private val state = MutableStateFlow(false)

  override fun markAlive() {
    state.value = true
  }

  override fun markDead() {
    state.value = false
  }

  override fun status(): StateFlow<Boolean> {
    return state
  }
}
