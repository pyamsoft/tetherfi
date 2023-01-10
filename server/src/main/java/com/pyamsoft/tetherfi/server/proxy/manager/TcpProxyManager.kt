package com.pyamsoft.tetherfi.server.proxy.manager

import androidx.annotation.CheckResult
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.pydroid.util.ifNotCancellation
import com.pyamsoft.tetherfi.server.proxy.SharedProxy
import com.pyamsoft.tetherfi.server.proxy.session.ProxySession
import com.pyamsoft.tetherfi.server.proxy.session.tcp.TcpProxyData
import com.pyamsoft.tetherfi.server.proxy.session.tcp.mempool.ManagedMemPool
import com.pyamsoft.tetherfi.server.proxy.session.tcp.mempool.MemPool
import io.ktor.network.sockets.ServerSocket
import io.ktor.network.sockets.SocketAddress
import io.ktor.network.sockets.SocketBuilder
import javax.inject.Provider
import kotlinx.coroutines.CoroutineDispatcher

internal class TcpProxyManager
internal constructor(
    private val session: ProxySession<TcpProxyData>,
    private val memPoolProvider: Provider<ManagedMemPool<ByteArray>>,
    dispatcher: CoroutineDispatcher,
    port: Int,
) :
    BaseProxyManager<ServerSocket>(
        SharedProxy.Type.TCP,
        dispatcher,
        port,
    ) {

  private var pool: ManagedMemPool<ByteArray>? = null

  @CheckResult
  private fun ensureMemPool(): MemPool<ByteArray> {
    pool =
        pool ?: memPoolProvider.get().requireNotNull().also { debugLog("Provide new MemPool: $it") }
    return pool.requireNotNull()
  }

  override suspend fun openServer(
      builder: SocketBuilder,
      localAddress: SocketAddress
  ): ServerSocket {
    return builder
        .tcp()
        .bind(
            localAddress = localAddress,
        )
  }

  override suspend fun runServer(server: ServerSocket) {
    // Do not use client.use() here or it will close too early
    val connection = server.accept()

    try {
      session.exchange(
          data =
              TcpProxyData(
                  runtime =
                      TcpProxyData.Runtime(
                          connection = connection,
                      ),
                  environment =
                      TcpProxyData.Environment(
                          memPool = ensureMemPool(),
                      ),
              ),
      )
    } catch (e: Throwable) {
      e.ifNotCancellation { errorLog(e, "Error during runSession") }
    } finally {
      // Always close the socket
      connection.dispose()
    }
  }

  override suspend fun onServerClosed() {
    pool?.dispose()
    pool = null
  }
}
