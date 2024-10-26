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

package com.pyamsoft.tetherfi.server.proxy

import androidx.annotation.CheckResult
import com.pyamsoft.tetherfi.core.Timber
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultServerDispatcherFactory
@Inject
internal constructor(
) : ServerDispatcher.Factory {

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
    val dispatcher = newThreadDispatcher()

    return DefaultServerDispatchers(
        primary = dispatcher.limitDispatcher(nThreads = PRIMARY_THREAD_COUNT),
        sideEffect = dispatcher.limitDispatcher(nThreads = SIDE_EFFECT_THREAD_COUNT),
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

  companion object {

    private val CORE_COUNT = Runtime.getRuntime().availableProcessors()

    // Side effect amount is the half number of CPU cores
    private val SIDE_EFFECT_THREAD_COUNT = CORE_COUNT / 2

    // Primary is 2X CPU cores
    private val PRIMARY_THREAD_COUNT = CORE_COUNT * 2

  }
}
