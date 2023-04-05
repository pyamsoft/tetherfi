package com.pyamsoft.tetherfi.service.lock

import android.annotation.SuppressLint
import android.content.Context
import android.os.PowerManager
import androidx.annotation.CheckResult
import androidx.core.content.getSystemService
import com.pyamsoft.pydroid.core.ThreadEnforcer
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.tetherfi.service.ServicePreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class WakeLocker
@Inject
internal constructor(
    enforcer: ThreadEnforcer,
    context: Context,
    private val preferences: ServicePreferences,
) : AbstractLocker() {

  private val mutex = Mutex()
  private val wakeLockTag = getWakeLockTag(context.packageName)

  private val wakeLock by lazy {
    enforcer.assertOffMainThread()

    val powerManager = context.getSystemService<PowerManager>().requireNotNull()
    return@lazy powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, wakeLockTag)
  }

  // Double check because we are also wrapped in a mutex
  private var wakeAcquired = AtomicBoolean(false)

  @SuppressLint("WakelockTimeout")
  override suspend fun acquireLock() =
      mutex.withLock {
        if (!wakeAcquired.getAndSet(true)) {
          Timber.d("####################################")
          Timber.d("Acquire CPU wakelock: $wakeLockTag")
          Timber.d("####################################")
          wakeLock.acquire()
        }
      }

  override suspend fun releaseLock() =
      mutex.withLock {
        if (wakeAcquired.getAndSet(false)) {
          Timber.d("####################################")
          Timber.d("Release CPU wakelock: $wakeLockTag")
          Timber.d("####################################")
          wakeLock.release()
        }
      }

  override suspend fun isEnabled(): Boolean {
    return preferences.listenForWakeLockChanges().first()
  }

  companion object {

    @JvmStatic
    @CheckResult
    private fun getWakeLockTag(name: String): String {
      return "${name}:PROXY_WAKE_LOCK"
    }
  }
}
