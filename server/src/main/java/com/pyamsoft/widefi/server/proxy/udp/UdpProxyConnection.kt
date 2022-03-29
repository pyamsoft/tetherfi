package com.pyamsoft.widefi.server.proxy.udp

import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.util.ifNotCancellation
import com.pyamsoft.widefi.server.ConnectionEvent
import com.pyamsoft.widefi.server.ErrorEvent
import com.pyamsoft.widefi.server.proxy.SharedProxy
import com.pyamsoft.widefi.server.proxy.connector.BaseProxyConnection
import com.pyamsoft.widefi.server.proxy.tagSocket
import com.pyamsoft.widefi.server.status.StatusBroadcast
import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.BoundDatagramSocket
import io.ktor.network.sockets.Datagram
import io.ktor.network.sockets.aSocket
import kotlinx.coroutines.CoroutineDispatcher
import timber.log.Timber

internal class UdpProxyConnection(
    private val port: Int,
    private val dispatcher: CoroutineDispatcher,
    private val errorBus: EventBus<ErrorEvent>,
    private val connectionBus: EventBus<ConnectionEvent>,
    status: StatusBroadcast,
    proxyDebug: Boolean,
) :
    BaseProxyConnection<BoundDatagramSocket, Datagram>(
        SharedProxy.Type.UDP,
        status,
        dispatcher,
        proxyDebug,
    ) {

  override fun openServerSocket(): BoundDatagramSocket {
    tagSocket()

    return aSocket(ActorSelectorManager(context = dispatcher))
        .udp()
        .bind(
            localAddress =
                getServerAddress(
                    port = port,
                ),
        )
  }

  override suspend fun acceptClientSocket(server: BoundDatagramSocket): Datagram {
    return server.receive()
  }

  override suspend fun handleSocketSession(client: Datagram) {
    val session =
        UdpSession(
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
