package com.pyamsoft.tetherfi.service

import androidx.annotation.CheckResult

interface ServicePreferences {

  @CheckResult suspend fun keepWakeLock(): Boolean

  suspend fun setWakeLock(keep: Boolean)
}
