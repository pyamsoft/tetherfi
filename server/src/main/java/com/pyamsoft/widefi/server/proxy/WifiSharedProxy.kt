package com.pyamsoft.widefi.server.proxy

import androidx.annotation.CheckResult
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.widefi.server.BaseServer
import com.pyamsoft.widefi.server.ConnectionEvent
import com.pyamsoft.widefi.server.ErrorEvent
import com.pyamsoft.widefi.server.proxy.connector.ProxyManager
import com.pyamsoft.widefi.server.status.RunningStatus
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber

@Singleton
internal class WifiSharedProxy
@Inject
internal constructor(
    @Named("proxy_debug") private val proxyDebug: Boolean,
    private val errorBus: EventBus<ErrorEvent>,
    private val connectionBus: EventBus<ConnectionEvent>,
    status: ProxyStatus,
) : BaseServer(status), SharedProxy {

  private val mutex = Mutex()
  private val jobs = mutableListOf<ProxyJob>()

  /** Dispatcher backed by its own thread group */
  private val dispatcher by lazy { Executors.newCachedThreadPool().asCoroutineDispatcher() }

  /** We own our own scope here because the proxy lifespan is separate */
  private val scope by lazy { CoroutineScope(context = dispatcher) }

  /**
   * Get the port for the proxy
   *
   * TODO Can be configured via SharedPreferences
   */
  @CheckResult
  private suspend fun getPort(): Int =
      withContext(context = Dispatchers.IO) {
        return@withContext 8228
      }

  @CheckResult
  private fun CoroutineScope.loopUdp(port: Int): ProxyJob {
    val udp =
        ProxyManager.udp(
            port = port,
            status = status,
            errorBus = errorBus,
            connectionBus = connectionBus,
            dispatcher = dispatcher,
            proxyDebug = proxyDebug,
        )

    Timber.d("Begin UDP proxy server loop")
    val job = launch(context = dispatcher) { udp.loop(this) }
    return ProxyJob(type = SharedProxy.Type.UDP, job = job)
  }

  @CheckResult
  private fun CoroutineScope.loopTcp(port: Int): ProxyJob {
    val tcp =
        ProxyManager.tcp(
            port = port,
            status = status,
            errorBus = errorBus,
            connectionBus = connectionBus,
            dispatcher = dispatcher,
            proxyDebug = proxyDebug,
        )

    Timber.d("Begin TCP proxy server loop")
    val job = launch(context = dispatcher) { tcp.loop(this) }
    return ProxyJob(type = SharedProxy.Type.TCP, job = job)
  }

  private suspend fun shutdown() {
    clearJobs()

    // Clear busses
    errorBus.send(ErrorEvent.Clear)
    connectionBus.send(ConnectionEvent.Clear)
  }

  private suspend fun clearJobs() {
    Timber.d("Cancelling jobs")
    mutex.withLock { jobs.removeEach { it.job.cancel() } }
  }

  override fun start() {
    scope.launch(context = dispatcher) {
      shutdown()

      val port = getPort()

      status.set(RunningStatus.Starting)

      coroutineScope {
        val tcp = loopTcp(port = port)
        val udp = loopUdp(port = port)

        mutex.withLock {
          jobs.add(tcp)
          jobs.add(udp)
        }

        Timber.d("Started proxy server on port: $port")
      }
    }
  }

  override fun stop() {
    scope.launch(context = dispatcher) {
      Timber.d("Stopping proxy server")

      status.set(RunningStatus.Stopping)

      shutdown()

      status.set(RunningStatus.NotRunning)
    }
  }

  override suspend fun onErrorEvent(block: (ErrorEvent) -> Unit) {
    return errorBus.onEvent { block(it) }
  }

  override suspend fun onConnectionEvent(block: (ConnectionEvent) -> Unit) {
    return connectionBus.onEvent { block(it) }
  }

  private inline fun <T> MutableList<T>.removeEach(block: (T) -> Unit) {
    while (this.isNotEmpty()) {
      block(this.removeFirst())
    }
  }

  private data class ProxyJob(
      val type: SharedProxy.Type,
      val job: Job,
  )
}
