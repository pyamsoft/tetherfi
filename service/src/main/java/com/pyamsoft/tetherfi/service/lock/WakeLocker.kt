/*
 * Copyright 2024 pyamsoft
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

package com.pyamsoft.tetherfi.service.lock

import android.annotation.SuppressLint
import android.content.Context
import android.os.PowerManager
import androidx.annotation.CheckResult
import androidx.core.content.getSystemService
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.tetherfi.core.Timber
import com.pyamsoft.tetherfi.server.StatusPreferences
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Singleton
internal class WakeLocker
@Inject
internal constructor(
    context: Context,
    private val statusPreferences: StatusPreferences,
) : AbstractLocker() {

  private val powerManager by lazy {
    context.applicationContext.getSystemService<PowerManager>().requireNotNull()
  }

  private val mutex = Mutex()
  private val tag = createTag(context.applicationContext.packageName)
  private val wakeAcquired = MutableStateFlow(false)

  private var lock: PowerManager.WakeLock? = null

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

    return powerManager.newWakeLock(wakeLockLevel, tag)
  }

  @SuppressLint("Wakelock")
  private fun killOldWakeLock() {
    lock?.also { l ->
      Timber.w { "LOCK IS STILL ALIVE. RELEASE OLD LOCK" }
      try {
        l.release()
      } catch (e: Throwable) {
        Timber.e(e) { "Unable to release old wakelock" }
      }
    }
  }

  @SuppressLint("WakelockTimeout")
  override suspend fun acquireLock() =
      withContext(context = Dispatchers.Default) {
        withContext(context = NonCancellable) {
          mutex.withLock {
            if (wakeAcquired.compareAndSet(expect = false, update = true)) {
              Timber.d { "####################################" }
              Timber.d { "Acquire CPU wakelock: $tag" }
              Timber.d { "####################################" }
              killOldWakeLock()
              createWakeLock()
                  .also { lock = it }
                  .also { l ->
                    try {
                      l.acquire()
                    } catch (e: Throwable) {
                      Timber.e(e) { "Unable to acquire wakelock" }
                    }
                  }
            }
          }
        }
      }

  override suspend fun releaseLock() =
      withContext(context = Dispatchers.Default) {
        withContext(context = NonCancellable) {
          mutex.withLock {
            if (wakeAcquired.compareAndSet(expect = true, update = false)) {
              Timber.d { "####################################" }
              Timber.d { "Release CPU wakelock: $tag" }
              Timber.d { "####################################" }
              lock?.also { l ->
                try {
                  l.release()
                } catch (e: Throwable) {
                  Timber.e(e) { "Unable to release wakelock" }
                }
              }
              lock = null
            }
          }
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
