package com.pyamsoft.tetherfi.server.proxy.manager

import androidx.annotation.CheckResult
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.pydroid.util.ifNotCancellation
import com.pyamsoft.tetherfi.server.proxy.SharedProxy
import com.pyamsoft.tetherfi.server.proxy.session.DestinationInfo
import com.pyamsoft.tetherfi.server.proxy.session.ProxySession
import com.pyamsoft.tetherfi.server.proxy.session.udp.UdpProxyData
import com.pyamsoft.tetherfi.server.proxy.session.udp.tracker.KeyedObjectProducer
import com.pyamsoft.tetherfi.server.proxy.session.udp.tracker.ManagedKeyedObjectProducer
import io.ktor.network.sockets.BoundDatagramSocket
import io.ktor.network.sockets.ConnectedDatagramSocket
import io.ktor.network.sockets.Datagram
import io.ktor.network.sockets.SocketAddress
import io.ktor.network.sockets.SocketBuilder
import javax.inject.Provider
import kotlinx.coroutines.CoroutineDispatcher

internal class UdpProxyManager
internal constructor(
    private val session: ProxySession<UdpProxyData>,
    private val connectionProducerProvider:
        Provider<ManagedKeyedObjectProducer<DestinationInfo, ConnectedDatagramSocket>>,
    proxyDebug: Boolean,
    port: Int,
    dispatcher: CoroutineDispatcher,
) :
    BaseProxyManager<BoundDatagramSocket, Datagram>(
        SharedProxy.Type.UDP,
        dispatcher,
        proxyDebug,
        port,
    ) {

  private var connectionProducer:
      ManagedKeyedObjectProducer<DestinationInfo, ConnectedDatagramSocket>? =
      null

  @CheckResult
  private fun ensureConnectionProducer():
      KeyedObjectProducer<DestinationInfo, ConnectedDatagramSocket> {
    connectionProducer =
        connectionProducer
            ?: connectionProducerProvider.get().requireNotNull().also {
              debugLog("Provide new ConnectionProducer: $it")
            }
    return connectionProducer.requireNotNull()
  }

  override suspend fun openServer(
      builder: SocketBuilder,
      localAddress: SocketAddress
  ): BoundDatagramSocket {
    return builder
        .udp()
        .bind(
            localAddress = localAddress,
        )
  }

  override suspend fun createSession(server: BoundDatagramSocket): Datagram {
    return server.receive()
  }

  override suspend fun runSession(server: BoundDatagramSocket, instance: Datagram) {
    try {
      session.exchange(
          data =
              UdpProxyData(
                  runtime =
                      UdpProxyData.Runtime(
                          proxy = server,
                          initialPacket = instance,
                      ),
                  environment =
                      UdpProxyData.Environment(
                          connectionProducer = ensureConnectionProducer(),
                      ),
              ),
      )
    } catch (e: Throwable) {
      e.ifNotCancellation { errorLog(e, "Error during session") }
    }
  }

  override suspend fun onServerClosed() {
    connectionProducer?.dispose()
    connectionProducer = null
  }
}
