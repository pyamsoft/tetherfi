package com.pyamsoft.tetherfi.tile

import androidx.annotation.CheckResult
import kotlinx.coroutines.flow.StateFlow

interface TileStatus {

  fun markAlive()

  fun markDead()

  @CheckResult fun status(): StateFlow<Boolean>
}
