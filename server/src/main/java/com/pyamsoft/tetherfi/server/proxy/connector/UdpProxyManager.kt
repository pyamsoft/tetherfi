package com.pyamsoft.tetherfi.server.proxy.connector

import com.pyamsoft.pydroid.core.Enforcer
import com.pyamsoft.pydroid.util.ifNotCancellation
import com.pyamsoft.tetherfi.server.proxy.SharedProxy
import com.pyamsoft.tetherfi.server.proxy.session.ProxySession
import com.pyamsoft.tetherfi.server.proxy.session.data.UdpProxyData
import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.BoundDatagramSocket
import io.ktor.network.sockets.Datagram
import io.ktor.network.sockets.aSocket
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

internal class UdpProxyManager
internal constructor(
    private val port: Int,
    private val dispatcher: CoroutineDispatcher,
    private val session: ProxySession<UdpProxyData>,
    proxyDebug: Boolean,
) :
    BaseProxyManager<BoundDatagramSocket>(
        SharedProxy.Type.UDP,
        proxyDebug,
    ) {

  private suspend fun runClientSession(server: BoundDatagramSocket, initialDatagram: Datagram) {
    try {
      session.exchange(
          data =
              UdpProxyData(
                  proxy = server,
                  initialPacket = initialDatagram,
              ),
      )
    } catch (e: Throwable) {
      e.ifNotCancellation { Timber.e(e, "${proxyType.name} Error during session") }
    }
  }

  override suspend fun openServer(): BoundDatagramSocket {
    return aSocket(ActorSelectorManager(context = dispatcher))
        .udp()
        .bind(
            localAddress =
                getServerAddress(
                    port = port,
                ),
        )
  }

  override suspend fun CoroutineScope.newSession(server: BoundDatagramSocket) {
    Enforcer.assertOffMainThread()

    val scope = this

    // UDP is a stateless connection, so as long as we are not blocking things, we can use a single
    // Proxy connection for all UDP sessions
    val datagram = server.receive()

    scope.launch(context = dispatcher) { runClientSession(server, datagram) }
  }

  override suspend fun onServerClosed() {
    session.finish()
  }
}
