package com.pyamsoft.widefi.server.proxy

import androidx.annotation.CheckResult
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.widefi.server.BaseServer
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
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber

@Singleton
internal class WifiSharedProxy
@Inject
internal constructor(
    private val errorBus: EventBus<ErrorEvent>,
    @Named("proxy_debug") private val proxyDebug: Boolean,
    status: ProxyStatus,
) : BaseServer(status), SharedProxy {

  private val dispatcher by lazy { Executors.newCachedThreadPool().asCoroutineDispatcher() }
  private val mutex = Mutex()
  private val jobs = mutableListOf<ProxyJob>()

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
            dispatcher = dispatcher,
            proxyDebug = proxyDebug,
        )

    Timber.d("Begin UDP proxy server loop")
    val job = launch(context = dispatcher) { udp.loop() }
    return ProxyJob(type = SharedProxy.Type.UDP, job = job)
  }

  @CheckResult
  private fun CoroutineScope.loopTcp(port: Int): ProxyJob {
    val tcp =
        ProxyManager.tcp(
            port = port,
            status = status,
            errorBus = errorBus,
            dispatcher = dispatcher,
            proxyDebug = proxyDebug,
        )

    Timber.d("Begin TCP proxy server loop")
    val job = launch(context = dispatcher) { tcp.loop() }
    return ProxyJob(type = SharedProxy.Type.TCP, job = job)
  }

  private suspend fun shutdown() {
    clearJobs()
    dispatcher.cancelChildren()
  }

  private suspend fun clearJobs() {
    Timber.d("Cancelling jobs")
    mutex.withLock { jobs.removeEach { it.job.cancel() } }
  }

  override suspend fun start() =
      withContext(context = Dispatchers.IO) {
        shutdown()

        val port = getPort()

        status.set(RunningStatus.Starting)

        val tcp = loopTcp(port = port)
        val udp = loopUdp(port = port)

        mutex.withLock {
          jobs.add(tcp)
          jobs.add(udp)
        }

        Timber.d("Started proxy server on port: $port")
      }

  override suspend fun stop() =
      withContext(context = Dispatchers.IO) {
        Timber.d("Stopping proxy server")

        status.set(RunningStatus.Stopping)

        shutdown()

        status.set(RunningStatus.NotRunning)
      }

  override suspend fun onErrorEvent(block: (ErrorEvent) -> Unit) {
    return errorBus.onEvent { block(it) }
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
