/*
 * Copyright 2023 pyamsoft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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
import com.pyamsoft.pydroid.core.ThreadEnforcer
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.tetherfi.service.ServicePreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class LockerImpl
@Inject
internal constructor(
    enforcer: ThreadEnforcer,
    context: Context,
    private val preferences: ServicePreferences,
) : Locker {

  private val wakeLockTag = getWakeLockTag(context.packageName)
  private val mutex = Mutex()

  private val wakeLock by lazy {
    enforcer.assertOffMainThread()

    val powerManager = context.getSystemService<PowerManager>().requireNotNull()
    return@lazy powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, wakeLockTag)
  }

  private var acquired = false

  @SuppressLint("WakelockTimeout")
  private suspend fun acquireWakelock() {
    mutex.withLock {
      if (!acquired) {
        Timber.d("####################################")
        Timber.d("Acquire CPU wakelock: $wakeLockTag")
        Timber.d("####################################")
        wakeLock.acquire()
        acquired = true
      }
    }
  }

  private suspend fun releaseWakelock() {
    mutex.withLock {
      if (acquired) {
        Timber.d("####################################")
        Timber.d("Release CPU wakelock: $wakeLockTag")
        Timber.d("####################################")
        wakeLock.release()
        acquired = false
      }
    }
  }

  override suspend fun acquire() =
      withContext(context = Dispatchers.IO + NonCancellable) {
        releaseWakelock()

        if (preferences.listenForWakeLockChanges().first()) {
          acquireWakelock()
        }
      }

  override suspend fun release() =
      withContext(context = Dispatchers.IO + NonCancellable) { releaseWakelock() }

  companion object {

    @JvmStatic
    @CheckResult
    private fun getWakeLockTag(name: String): String {
      return "${name}:PROXY_WAKE_LOCK"
    }
  }
}
