/*
 * Copyright 2025 pyamsoft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pyamsoft.tetherfi.server.lock

import android.annotation.SuppressLint
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.ConnectivityManager
import android.os.PowerManager
import androidx.annotation.CheckResult
import androidx.core.content.getSystemService
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.tetherfi.core.Timber
import com.pyamsoft.tetherfi.server.StatusPreferences
import com.pyamsoft.tetherfi.server.TweakPreferences
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

@Singleton
internal class WakeLocker
@Inject
internal constructor(
    context: Context,
    private val statusPreferences: StatusPreferences,
    private val tweakPreferences: TweakPreferences,
) : AbstractLocker() {

  private val powerManager by lazy {
    context.applicationContext.getSystemService<PowerManager>().requireNotNull()
  }

  private val tag by lazy { createTag(context.applicationContext.packageName) }

  @CheckResult
  @Suppress("DEPRECATION")
  private fun resolveValidBrightWakeLockLevel(): Int {
    if (powerManager.isWakeLockLevelSupported(PowerManager.SCREEN_BRIGHT_WAKE_LOCK)) {
      Timber.d { "KeepScreenOn: Create new wake lock with SCREEN_BRIGHT_WAKE_LOCK" }
      return PowerManager.SCREEN_BRIGHT_WAKE_LOCK
    } else {
      Timber.d {
        "KeepScreenOn: Create new wake lock with PARTIAL_WAKE_LOCK (SCREEN_BRIGHT_WAKE_LOCK unsupported)"
      }
      return PowerManager.PARTIAL_WAKE_LOCK
    }
  }

  @CheckResult
  private suspend fun createWakeLock(): PowerManager.WakeLock {
    val isKeepScreenOn = statusPreferences.listenForKeepScreenOn().first()

    val wakeLockLevel: Int
    if (isKeepScreenOn) {
      wakeLockLevel = resolveValidBrightWakeLockLevel()
    } else {
      Timber.d { "Create new wake lock with PARTIAL_WAKE_LOCK" }
      wakeLockLevel = PowerManager.PARTIAL_WAKE_LOCK
    }

    return powerManager.newWakeLock(wakeLockLevel, tag).apply {
      // We will count our own refs
      setReferenceCounted(false)
    }
  }

  override suspend fun createLock(): Locker.Lock =
      withContext(context = Dispatchers.Default) {
        val isWakeLockEnabled = tweakPreferences.listenForWakeLock().first()
        if (isWakeLockEnabled) {
          val wakeLock = createWakeLock()
          return@withContext Lock(wakeLock, tag)
        }

        return@withContext NoopLock
      }

  internal class Lock(
      private val wakeLock: PowerManager.WakeLock,
      lockTag: String,
  ) : AbstractLock(lockType = "CPU", lockTag = lockTag) {

    override suspend fun isHeld(): Boolean {
      return wakeLock.isHeld
    }

    @SuppressLint("WakelockTimeout")
    override suspend fun onAcquireLock() {
      wakeLock.acquire()
    }

    override suspend fun onReleaseLock() {
      wakeLock.release()
    }
  }

  companion object {

    @JvmStatic
    @CheckResult
    private fun createTag(name: String): String {
      return "${name}:PROXY_WAKE_LOCK"
    }
  }
}
