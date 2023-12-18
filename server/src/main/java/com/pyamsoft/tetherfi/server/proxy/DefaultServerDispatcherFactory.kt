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
  @OptIn(ExperimentalCoroutinesApi::class)
  private fun newThreadDispatcher(nThreads: Int): CoroutineDispatcher {
    val isUnlimited = nThreads <= 0
    val logThreads = if (isUnlimited) "UNLIMITED" else nThreads
    Timber.d { "Create new executor CoroutineDispatcher n=(${logThreads})" }

    // Used cached pool so we recycle
    return Executors.newCachedThreadPool { task ->
          Thread(task).apply {
            // Daemonize threads so JVM can exit
            isDaemon = true

            // Low priority
            priority = Thread.MIN_PRIORITY
          }
        }
        .asCoroutineDispatcher()
        .run {
          if (isUnlimited) {
            // Unlimited, run free!!
            this
          } else {
            // And limit concurrency
            limitedParallelism(parallelism = nThreads)
          }
        }
  }

  @CheckResult
  private fun ServerPerformanceLimit.toDispatcher(): CoroutineDispatcher =
      newThreadDispatcher(
          nThreads = coroutineLimit,
      )

  override suspend fun resolve(): ServerDispatcher {
    val primaryLimit = flow.first()

    val halfCpu = ServerPerformanceLimit.Defaults.BOUND_N_CPU.coroutineLimit / 4
    val sideEffectThreads = 2.coerceAtLeast(halfCpu)

    val isPrimaryUnbound = primaryLimit.coroutineLimit <= 0

    return DefaultServerDispatchers(
        isPrimaryUnbound = isPrimaryUnbound,
        primary = primaryLimit.toDispatcher(),

        // TODO: Scale somehow based on primary? or just keep as half_cpu OR 4
        sideEffect =
            newThreadDispatcher(
                nThreads = sideEffectThreads,
            ),
    )
  }

  private data class DefaultServerDispatchers(
      override val primary: CoroutineDispatcher,
      override val sideEffect: CoroutineDispatcher,
      override val isPrimaryUnbound: Boolean,
  ) : ServerDispatcher
}
