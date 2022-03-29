package com.pyamsoft.widefi.server.proxy.tcp

import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.util.ifNotCancellation
import com.pyamsoft.widefi.server.ConnectionEvent
import com.pyamsoft.widefi.server.ErrorEvent
import com.pyamsoft.widefi.server.proxy.SharedProxy
import com.pyamsoft.widefi.server.proxy.connector.BaseProxyConnection
import com.pyamsoft.widefi.server.proxy.tagSocket
import com.pyamsoft.widefi.server.status.StatusBroadcast
import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.ServerSocket
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import kotlinx.coroutines.CoroutineDispatcher
import timber.log.Timber

internal class TcpProxyConnection(
    private val port: Int,
    private val dispatcher: CoroutineDispatcher,
    private val errorBus: EventBus<ErrorEvent>,
    private val connectionBus: EventBus<ConnectionEvent>,
    status: StatusBroadcast,
    proxyDebug: Boolean,
) :
    BaseProxyConnection<ServerSocket, Socket>(
        SharedProxy.Type.TCP,
        status,
        dispatcher,
        proxyDebug,
    ) {

  override fun openServerSocket(): ServerSocket {
    tagSocket()

    return aSocket(ActorSelectorManager(context = dispatcher))
        .tcp()
        .bind(
            localAddress =
                getServerAddress(
                    port = port,
                ),
        )
  }

  override suspend fun acceptClientSocket(server: ServerSocket): Socket {
    return server.accept()
  }

  override suspend fun handleSocketSession(client: Socket) {
    val session =
        TcpSession(
            dispatcher = dispatcher,
            errorBus = errorBus,
            connectionBus = connectionBus,
            proxyDebug = proxyDebug,
        )

    try {
      session.exchange(client)
    } catch (e: Throwable) {
      e.ifNotCancellation { Timber.e(e, "${proxyType.name} Error during session") }
    }
  }
}
