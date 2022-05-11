package com.pyamsoft.widefi.server.proxy.connector

import androidx.annotation.CheckResult
import com.pyamsoft.pydroid.util.ifNotCancellation
import com.pyamsoft.widefi.server.proxy.SharedProxy
import com.pyamsoft.widefi.server.proxy.session.ProxySession
import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.ServerSocket
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.UnixSocketAddress
import io.ktor.network.sockets.aSocket
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

internal class TcpProxyManager
internal constructor(
    private val port: Int,
    private val dispatcher: CoroutineDispatcher,
    private val factory: ProxySession.Factory<Socket>,
    proxyDebug: Boolean,
) :
    BaseProxyManager<ServerSocket, Socket>(
        SharedProxy.Type.TCP,
        dispatcher,
        proxyDebug,
    ) {

  private val mutex = Mutex()
  private val clientMap = mutableMapOf<String, Boolean>()

  override fun openServer(): ServerSocket {
    return aSocket(ActorSelectorManager(context = dispatcher))
        .tcp()
        .bind(
            localAddress =
                getServerAddress(
                    port = port,
                ),
        )
  }

  override suspend fun acceptClient(server: ServerSocket): Socket {
    return server.accept().apply {
      val id = getClientId(this)

      mutex.withLock { clientMap[id] = true }
    }
  }

  override suspend fun runSession(client: Socket) {
    val session = factory.create()
    try {
      session.exchange(client)
    } catch (e: Throwable) {
      e.ifNotCancellation { Timber.e(e, "${proxyType.name} Error during session") }
    }
  }

  override suspend fun closeSession(client: Socket) {
    try {
      val id = getClientId(client)
      mutex.withLock { clientMap[id] = false }
    } finally {
      // Close the socket
      client.dispose()
    }
  }

  companion object {

    @JvmStatic
    @CheckResult
    private fun getClientId(client: Socket): String {
      return when (val addr = client.remoteAddress) {
        is InetSocketAddress -> addr.hostname
        is UnixSocketAddress -> throw IllegalStateException("Cannot get client ID from Unix socket")
      }
    }
  }
}
