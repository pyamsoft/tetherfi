package com.pyamsoft.tetherfi.server.proxy

import androidx.annotation.CheckResult
import com.pyamsoft.tetherfi.core.Timber
import com.pyamsoft.tetherfi.server.ConfigPreferences
import com.pyamsoft.tetherfi.server.ServerPerformanceLimit
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.first

@Singleton
class DefaultServerDispatcherFactory
@Inject
internal constructor(
    preferences: ConfigPreferences,
) : ServerDispatcher.Factory {

  private val flow by lazy { preferences.listenForPerformanceLimits() }

  /** Make a new thread dispatcher using Daemon threads */
  @CheckResult
  private fun newThreadDispatcher(): CoroutineDispatcher {
    Timber.d { "Create a new cachedThreadPool dispatcher for Server" }
    return Executors.newCachedThreadPool { task ->
          Thread(task).apply {
            // Daemonize threads so JVM can exit
            isDaemon = true

            // Low priority
            priority = Thread.MIN_PRIORITY
          }
        }
        .asCoroutineDispatcher()
  }

  @CheckResult
  @OptIn(ExperimentalCoroutinesApi::class)
  private fun CoroutineDispatcher.limitDispatcher(nThreads: Int): CoroutineDispatcher {
    val isUnlimited = nThreads <= 0
    return run {
      if (isUnlimited) {
        // Unlimited, run free!!
        Timber.d { "Server CoroutineDispatcher is Unlimited" }
        this
      } else {
        Timber.d { "Limit Server CoroutineDispatcher n=(${nThreads})" }
        // And limit concurrency
        limitedParallelism(parallelism = nThreads)
      }
    }
  }

  override suspend fun create(): ServerDispatcher {
    val primaryLimit = flow.first()

    val halfCpu = ServerPerformanceLimit.Defaults.BOUND_N_CPU.coroutineLimit / 4
    val sideEffectThreads = 2.coerceAtLeast(halfCpu)

    val dispatcher = newThreadDispatcher()

    return DefaultServerDispatchers(
        primary = dispatcher.limitDispatcher(nThreads = primaryLimit.coroutineLimit),

        // TODO: Scale somehow based on primary? or just keep as half_cpu OR 4
        sideEffect = dispatcher.limitDispatcher(nThreads = sideEffectThreads),
    )
  }

  private data class DefaultServerDispatchers(
      override val primary: CoroutineDispatcher,
      override val sideEffect: CoroutineDispatcher,
  ) : ServerDispatcher
}
