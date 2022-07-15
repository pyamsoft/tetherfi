package com.pyamsoft.tetherfi.service.lock

import android.annotation.SuppressLint
import android.content.Context
import android.os.PowerManager
import androidx.annotation.CheckResult
import androidx.core.content.getSystemService
import com.pyamsoft.pydroid.core.Enforcer
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.tetherfi.service.ServicePreferences
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber

@Singleton
internal class LockerImpl
@Inject
internal constructor(
    context: Context,
    private val preferences: ServicePreferences,
) : Locker {

  private val wakeLockTag = getWakeLockTag(context.packageName)
  private val mutex = Mutex()

  private val wakeLock by lazy {
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
        Enforcer.assertOffMainThread()

        releaseWakelock()

        if (preferences.keepWakeLock()) {
          acquireWakelock()
        }
      }

  override suspend fun release() =
      withContext(context = Dispatchers.IO + NonCancellable) {
        Enforcer.assertOffMainThread()

        releaseWakelock()
      }

  companion object {

    @JvmStatic
    @CheckResult
    private fun getWakeLockTag(name: String): String {
      return "${name}:PROXY_WAKE_LOCK"
    }
  }
}
