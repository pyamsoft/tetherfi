package com.pyamsoft.tetherfi.service.foreground

import androidx.annotation.CheckResult
import com.pyamsoft.pydroid.bus.EventConsumer
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.tetherfi.server.event.ServerShutdownEvent
import com.pyamsoft.tetherfi.server.status.RunningStatus
import com.pyamsoft.tetherfi.server.widi.WiDiNetwork
import com.pyamsoft.tetherfi.server.widi.WiDiNetworkStatus
import com.pyamsoft.tetherfi.service.ServiceInternalApi
import com.pyamsoft.tetherfi.service.lock.Locker
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import timber.log.Timber

@Singleton
class ForegroundHandler
@Inject
internal constructor(
    @ServiceInternalApi private val locker: Locker,
    private val shutdownBus: EventConsumer<ServerShutdownEvent>,
    private val network: WiDiNetwork,
    private val status: WiDiNetworkStatus,
) {

  private val scope by lazy(LazyThreadSafetyMode.NONE) { MainScope() }

  /**
   * Don't cancel this job on destroy. It must listen for the final shutdown event fired from the
   * server
   */
  private var shutdownJob: Job? = null

  private var networkStatusJob: Job? = null
  private var proxyStatusJob: Job? = null

  private fun killJobs() {
    proxyStatusJob?.cancel()
    proxyStatusJob = null

    networkStatusJob?.cancel()
    networkStatusJob = null
  }

  @CheckResult
  private fun Job?.cancelAndReLaunch(block: suspend CoroutineScope.() -> Unit): Job {
    this?.cancel()
    return scope.launch(context = Dispatchers.Main, block = block)
  }

  fun bind(
      onShutdownService: () -> Unit,
  ) {
    // When shutdown events are received, we kill the service
    shutdownJob =
        shutdownJob.cancelAndReLaunch {
          Timber.d("Watching for Shutdown")
          shutdownBus.requireNotNull().onEvent {
            Timber.d("Shutdown event received!")
            onShutdownService()
          }
        }

    // Watch status of network
    networkStatusJob =
        networkStatusJob.cancelAndReLaunch {
          status.requireNotNull().onStatusChanged { s ->
            when (s) {
              is RunningStatus.Error -> {
                Timber.w("Server Server Error: ${s.message}")
                locker.release()
              }
              else -> Timber.d("Server status changed: $s")
            }
          }
        }

    // Watch status of proxy
    proxyStatusJob =
        proxyStatusJob.cancelAndReLaunch {
          status.requireNotNull().onProxyStatusChanged { s ->
            when (s) {
              is RunningStatus.Running -> {
                Timber.d("Proxy Server started!")
                locker.acquire()
              }
              is RunningStatus.Error -> {
                Timber.w("Proxy Server Error: ${s.message}")
                locker.release()
              }
              else -> Timber.d("Proxy status changed: $s")
            }
          }
        }
  }

  fun startProxy() {
    Timber.d("Start WiDi Network")
    network.start()
  }

  fun stopProxy() {
    Timber.d("Stop WiDi network")
    network.stop()

    // Launch a parent scope for all jobs
    scope.launch(context = Dispatchers.Main) {
      Timber.d("Destroy CPU wakelock")
      locker.release()
    }

    killJobs()
  }

  fun destroy() {
    stopProxy()

    shutdownJob?.cancel()
    shutdownJob = null
  }
}
