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
  private val tag = createTag(context.packageName)

  private val lock by lazy {
    enforcer.assertOffMainThread()

    val powerManager = context.getSystemService<PowerManager>().requireNotNull()
    return@lazy powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, tag)
  }

  // Double check because we are also wrapped in a mutex
  private var wakeAcquired = AtomicBoolean(false)

  @SuppressLint("WakelockTimeout")
  override suspend fun acquireLock() =
      withContext(context = NonCancellable) {
        withContext(context = Dispatchers.IO) {
          mutex.withLock {
            if (!wakeAcquired.getAndSet(true)) {
              Timber.d("####################################")
              Timber.d("Acquire CPU wakelock: $tag")
              Timber.d("####################################")
              lock.acquire()
            }
          }
        }
      }

  override suspend fun releaseLock() =
      withContext(context = NonCancellable) {
        withContext(context = Dispatchers.IO) {
          mutex.withLock {
            if (wakeAcquired.getAndSet(false)) {
              Timber.d("####################################")
              Timber.d("Release CPU wakelock: $tag")
              Timber.d("####################################")
              lock.release()
            }
          }
        }
      }

  override suspend fun isEnabled(): Boolean =
      withContext(context = Dispatchers.IO) { preferences.listenForWakeLockChanges().first() }

  companion object {

    @JvmStatic
    @CheckResult
    private fun createTag(name: String): String {
      return "${name}:PROXY_WAKE_LOCK"
    }
  }
}
