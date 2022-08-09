package com.pyamsoft.tetherfi.service

import androidx.annotation.CheckResult
import kotlinx.coroutines.flow.Flow

interface ServicePreferences {

  @CheckResult suspend fun listenForWakeLockChanges(): Flow<Boolean>

  suspend fun setWakeLock(keep: Boolean)
}
