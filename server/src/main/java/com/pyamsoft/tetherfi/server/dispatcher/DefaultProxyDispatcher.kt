package com.pyamsoft.tetherfi.server.dispatcher

import com.pyamsoft.pydroid.core.requireNotNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.newFixedThreadPoolContext
import timber.log.Timber

@Singleton
internal class DefaultProxyDispatcher @Inject internal constructor() : ProxyDispatcher {

  private var dispatcher: ExecutorCoroutineDispatcher? = null

  private fun killDispatcher(d: ExecutorCoroutineDispatcher) {
    Timber.d("Closing Proxy dispatcher $d")
    d.close()
    d.cancel()
  }

  @OptIn(DelicateCoroutinesApi::class)
  override fun ensureActiveDispatcher(): CoroutineDispatcher {
    dispatcher =
        dispatcher.let { d ->
          if (d == null || !d.isActive) {

            // Kill or close an old or inactive dispatcher
            d?.also { killDispatcher(it) }

            // We max parallelism at CORES * 2 to avoid
            // overallocating and system level CPU trashing
            val coreCount = Runtime.getRuntime().availableProcessors()
            val parallelism = coreCount * 2

            Timber.d(RuntimeException(), "Using Proxy limited dispatcher=$parallelism")
            return@let newFixedThreadPoolContext(parallelism, this::class.java.name)
          } else {
            return@let d
          }
        }

    return dispatcher.requireNotNull()
  }

  override fun shutdown() {
    dispatcher?.also { d ->
      Timber.d("Shutdown proxy dispatcher $d")
      killDispatcher(d)
    }
    dispatcher = null
  }
}
