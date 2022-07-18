package com.pyamsoft.tetherfi.server.proxy.manager

import androidx.annotation.CheckResult
import com.pyamsoft.pydroid.core.Enforcer
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.pydroid.util.ifNotCancellation
import com.pyamsoft.tetherfi.server.proxy.SharedProxy
import com.pyamsoft.tetherfi.server.proxy.session.ProxySession
import com.pyamsoft.tetherfi.server.proxy.session.data.TcpProxyData
import com.pyamsoft.tetherfi.server.proxy.session.tcp.mempool.ManagedMemPool
import com.pyamsoft.tetherfi.server.proxy.session.tcp.mempool.MemPool
import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.ServerSocket
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import javax.inject.Provider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

internal class TcpProxyManager
internal constructor(
    private val port: Int,
    private val dispatcher: CoroutineDispatcher,
    private val session: ProxySession<TcpProxyData>,
    private val memPoolProvider: Provider<ManagedMemPool<ByteArray>>,
    proxyDebug: Boolean,
) :
    BaseProxyManager<ServerSocket>(
        SharedProxy.Type.TCP,
        proxyDebug,
    ) {

  private var pool: ManagedMemPool<ByteArray>? = null

  @CheckResult
  private fun ensureMemPool(): MemPool<ByteArray> {
    pool =
        pool ?: memPoolProvider.get().requireNotNull().also { Timber.d("Provide new MemPool: $it") }
    return pool.requireNotNull()
  }

  private suspend fun runClientSession(client: Socket) {
    Enforcer.assertOffMainThread()

    try {
      session.exchange(
          data =
              TcpProxyData(
                  runtime =
                      TcpProxyData.Runtime(
                          proxy = client,
                      ),
                  environment =
                      TcpProxyData.Environment(
                          memPool = ensureMemPool(),
                      ),
              ),
      )
    } catch (e: Throwable) {
      e.ifNotCancellation { Timber.e(e, "${proxyType.name} Error during session") }
    } finally {
      // Always close the client
      client.dispose()
    }
  }

  override suspend fun openServer(): ServerSocket {
    return aSocket(ActorSelectorManager(context = dispatcher))
        .tcp()
        .bind(
            localAddress =
                getServerAddress(
                    port = port,
                ),
        )
  }

  override suspend fun CoroutineScope.newSession(server: ServerSocket) {
    Enforcer.assertOffMainThread()
    val scope = this

    // Do not use client.use() here or it will close too early
    val client = server.accept()

    // Launch a new coroutine here so we don't block the main loop for each client connection
    scope.launch(context = dispatcher) { runClientSession(client) }
  }

  override suspend fun onServerClosed() {
    pool?.dispose()
    pool = null
  }
}
