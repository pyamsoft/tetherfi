package com.pyamsoft.tetherfi.server.proxy.session.udp.tracker

import com.pyamsoft.tetherfi.server.ServerInternalApi
import com.pyamsoft.tetherfi.server.proxy.session.DestinationInfo
import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.ConnectedDatagramSocket
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.isClosed
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher

// Not singleton since this will be provided each time by a Provider<>
internal class UdpKeyedObjectProducer
@Inject
internal constructor(
    @ServerInternalApi private val dispatcher: CoroutineDispatcher,
) : UnboundedKeyedObjectProducer<DestinationInfo, ConnectedDatagramSocket>() {

  override fun isValid(value: ConnectedDatagramSocket): Boolean {
    return !value.isClosed
  }

  override fun make(key: DestinationInfo): ConnectedDatagramSocket {
    return aSocket(ActorSelectorManager(context = dispatcher))
        .udp()
        .connect(
            remoteAddress =
                InetSocketAddress(
                    hostname = key.hostName,
                    port = key.port,
                ),
        )
  }
}
