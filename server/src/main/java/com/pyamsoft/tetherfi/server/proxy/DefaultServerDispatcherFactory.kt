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

package com.pyamsoft.tetherfi.server.proxy

import androidx.annotation.CheckResult
import com.pyamsoft.tetherfi.core.Timber
import com.pyamsoft.tetherfi.server.ExpertPreferences
import com.pyamsoft.tetherfi.server.ServerPerformanceLimit
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first

@Singleton
class DefaultServerDispatcherFactory
@Inject
internal constructor(
    preferences: ExpertPreferences,
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

    // Side effect amount is the number of CPU cores
    val sideEffectThreads = ServerPerformanceLimit.Defaults.BOUND_N_CPU.coroutineLimit

    val dispatcher = newThreadDispatcher()

    return DefaultServerDispatchers(
        primary = dispatcher.limitDispatcher(nThreads = primaryLimit.coroutineLimit),
        sideEffect = dispatcher.limitDispatcher(nThreads = sideEffectThreads),
    )
  }

  private data class DefaultServerDispatchers(
      override val primary: CoroutineDispatcher,
      override val sideEffect: CoroutineDispatcher,
  ) : ServerDispatcher {

    private fun CoroutineDispatcher.shutdown() {
      val self = this
      if (self is ExecutorCoroutineDispatcher) {
        self.close()
      } else {
        self.cancel()
      }
    }

    override fun shutdown() {
      Timber.d { "Shutdown Primary Dispatcher" }
      primary.shutdown()

      Timber.d { "Shutdown SideEffect Dispatcher" }
      sideEffect.shutdown()
    }
  }
}
