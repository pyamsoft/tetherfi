package com.pyamsoft.tetherfi.server.proxy

import androidx.annotation.CheckResult
import com.pyamsoft.tetherfi.core.Timber
import com.pyamsoft.tetherfi.server.ConfigPreferences
import com.pyamsoft.tetherfi.server.ServerPerformanceLimit
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

@Singleton
class DefaultServerDispatcherFactory
@Inject
internal constructor(
    preferences: ConfigPreferences,
) : ServerDispatcher.Factory {

  private val flow by lazy { preferences.listenForPerformanceLimits().map { it.toDispatcher() } }

  // TODO: Scale?
  private val sideEffect by lazy { Executors.newFixedThreadPool(4).asCoroutineDispatcher() }

  @CheckResult
  private fun ServerPerformanceLimit.toDispatcher(): CoroutineDispatcher =
      when (this) {
        is ServerPerformanceLimit.LimitUnbound -> {
          Timber.d { "Create new Unbound ServerDispatcher" }

          // return
          Dispatchers.IO
        }
        else -> {
          Timber.d { "Create new bound=(${coroutineLimit}) ServerDispatcher" }

          // return
          Executors.newFixedThreadPool(coroutineLimit).asCoroutineDispatcher()
        }
      }

  override suspend fun resolve(): ServerDispatcher {
    return DefaultServerDispatchers(
        primary = flow.first(),
        sideEffect = sideEffect,
    )
  }

  private data class DefaultServerDispatchers(
      override val primary: CoroutineDispatcher,
      override val sideEffect: CoroutineDispatcher,
  ) : ServerDispatcher
}
