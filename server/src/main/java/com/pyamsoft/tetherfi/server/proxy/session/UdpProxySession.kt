package com.pyamsoft.tetherfi.server.proxy.session

import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.core.Enforcer
import com.pyamsoft.pydroid.util.ifNotCancellation
import com.pyamsoft.tetherfi.core.generateRandomId
import com.pyamsoft.tetherfi.server.event.ConnectionEvent
import com.pyamsoft.tetherfi.server.event.ErrorEvent
import com.pyamsoft.tetherfi.server.proxy.SharedProxy
import io.ktor.network.sockets.Datagram
import io.ktor.network.sockets.InetSocketAddress

internal class UdpProxySession
internal constructor(
    private val errorBus: EventBus<ErrorEvent>,
    private val connectionBus: EventBus<ConnectionEvent>,
    proxyDebug: Boolean,
) : BaseProxySession<UdpProxyOptions>(SharedProxy.Type.UDP, proxyDebug) {

  override suspend fun exchange(data: UdpProxyOptions) {
    Enforcer.assertOffMainThread()

    val packet = data.packet

    // Pull out the original packet's address data
    val address = packet.address
    if (address !is InetSocketAddress) {
      throw IllegalArgumentException("UDP Proxy does not handle Unix sockets")
    }

    // Craft the send packet from the data
    val sendPacket =
        Datagram(
            packet = packet.packet,
            address = address,
        )

    debugLog { "${proxyType.name} Forward datagram to destination: $sendPacket" }

    // Log connection
    connectionBus.send(
        ConnectionEvent.Udp(
            id = generateRandomId(),
            hostName = address.hostname,
            port = address.port,
        ),
    )

    // Send the data to the socket
    try {
      data.sender.send(sendPacket)
    } catch (e: Throwable) {
      e.ifNotCancellation {
        errorLog(e) { "${proxyType.name} Error during datagram forwarding: $sendPacket" }
        errorBus.send(
            ErrorEvent.Udp(
                id = generateRandomId(),
                hostName = address.hostname,
                port = address.port,
                throwable = e,
            ),
        )
      }
    }
  }
}
