package com.pyamsoft.tetherfi.server.proxy.manager

import androidx.annotation.CheckResult
import com.pyamsoft.pydroid.core.Enforcer
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.pydroid.util.ifNotCancellation
import com.pyamsoft.tetherfi.server.proxy.SharedProxy
import com.pyamsoft.tetherfi.server.proxy.session.ProxySession
import com.pyamsoft.tetherfi.server.proxy.session.tcp.TcpProxyData
import com.pyamsoft.tetherfi.server.proxy.session.tcp.mempool.ManagedMemPool
import com.pyamsoft.tetherfi.server.proxy.session.tcp.mempool.MemPool
import io.ktor.network.sockets.ServerSocket
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.SocketAddress
import io.ktor.network.sockets.SocketBuilder
import io.ktor.network.sockets.isClosed
import javax.inject.Provider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal class TcpProxyManager
internal constructor(
    private val session: ProxySession<TcpProxyData>,
    private val memPoolProvider: Provider<ManagedMemPool<ByteArray>>,
    private val dispatcher: CoroutineDispatcher,
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

  private suspend fun runSession(connection: Socket) = coroutineScope {
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
    }
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

  override suspend fun runServer(server: ServerSocket) = coroutineScope {
    Enforcer.assertOffMainThread()

    // In a loop, we wait for new TCP connections and then offload them to their own routine.
    while (isActive && !server.isClosed) {
      // We must close the connection in the launch{} after exchange is over
      val connection = server.accept()

      // Run this server loop off thread so we can handle multiple connections at once.
      launch(context = dispatcher) {
        try {
          runSession(connection)
        } finally {
          connection.dispose()
        }
      }
    }
  }

  override suspend fun onServerClosed() {
    pool?.dispose()
    pool = null
  }
}
