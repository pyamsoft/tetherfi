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

@Singleton
class DefaultServerDispatcherFactory
@Inject
internal constructor(
    preferences: ConfigPreferences,
) : ServerDispatcher.Factory {

  private val flow by lazy { preferences.listenForPerformanceLimits() }

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
    val primaryLimit = flow.first()
    return DefaultServerDispatchers(
        isPrimaryBound =
            primaryLimit is ServerPerformanceLimit.LimitUnbound || primaryLimit.coroutineLimit <= 0,
        primary = primaryLimit.toDispatcher(),
        sideEffect = sideEffect,
    )
  }

  private data class DefaultServerDispatchers(
      override val primary: CoroutineDispatcher,
      override val sideEffect: CoroutineDispatcher,
      override val isPrimaryBound: Boolean,
  ) : ServerDispatcher
}
