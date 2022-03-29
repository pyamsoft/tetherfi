package com.pyamsoft.widefi.server.proxy.connector

import com.pyamsoft.pydroid.util.ifNotCancellation
import com.pyamsoft.widefi.server.proxy.SharedProxy
import com.pyamsoft.widefi.server.proxy.connector.BaseProxyManager
import com.pyamsoft.widefi.server.proxy.session.ProxySession
import com.pyamsoft.widefi.server.proxy.tagSocket
import com.pyamsoft.widefi.server.status.StatusBroadcast
import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.BoundDatagramSocket
import io.ktor.network.sockets.Datagram
import io.ktor.network.sockets.aSocket
import kotlinx.coroutines.CoroutineDispatcher
import timber.log.Timber

internal class UdpProxyManager
internal constructor(
    private val port: Int,
    private val dispatcher: CoroutineDispatcher,
    private val factory: ProxySession.Factory<Datagram>,
    status: StatusBroadcast,
    proxyDebug: Boolean,
) :
    BaseProxyManager<BoundDatagramSocket, Datagram>(
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
    val session = factory.create()
    try {
      session.exchange(client)
    } catch (e: Throwable) {
      e.ifNotCancellation { Timber.e(e, "${proxyType.name} Error during session") }
    }
  }
}
