package com.pyamsoft.tetherfi.server.proxy.session

import androidx.annotation.CheckResult
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.core.Enforcer
import com.pyamsoft.pydroid.util.ifNotCancellation
import com.pyamsoft.tetherfi.core.generateRandomId
import com.pyamsoft.tetherfi.server.event.ConnectionEvent
import com.pyamsoft.tetherfi.server.event.ErrorEvent
import com.pyamsoft.tetherfi.server.proxy.SharedProxy
import com.pyamsoft.tetherfi.server.proxy.session.options.DatagramSender
import com.pyamsoft.tetherfi.server.proxy.session.options.UdpProxyOptions
import com.pyamsoft.tetherfi.server.proxy.session.urlfixer.UrlFixer
import io.ktor.network.sockets.Datagram
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.SocketAddress
import io.ktor.utils.io.core.ByteReadPacket

internal class UdpProxySession
internal constructor(
    private val errorBus: EventBus<ErrorEvent>,
    private val connectionBus: EventBus<ConnectionEvent>,
    urlFixers: Set<UrlFixer>,
    proxyDebug: Boolean,
) :
    BaseProxySession<UdpProxyOptions>(
        SharedProxy.Type.UDP,
        urlFixers,
        proxyDebug,
    ) {

  @CheckResult
  private fun craftSendPacket(
      packet: ByteReadPacket,
      hostName: String,
      port: Int,
  ): Datagram {
    return Datagram(
        packet = packet,
        address =
            InetSocketAddress(
                hostname = hostName,
                port = port,
            ),
    )
  }

  private suspend fun sendPacketToDestination(
      sender: DatagramSender,
      packet: Datagram,
      hostName: String,
      port: Int,
  ) {
    debugLog { "${proxyType.name} Forward datagram to destination: $hostName $port" }

    // Log connection
    connectionBus.send(
        ConnectionEvent.Udp(
            id = generateRandomId(),
            hostName = hostName,
            port = port,
        ),
    )

    // Send the data to the socket
    try {
      sender.send(packet)
    } catch (e: Throwable) {
      e.ifNotCancellation {
        errorLog(e) { "${proxyType.name} Error during datagram forwarding: $hostName $port" }
        errorBus.send(
            ErrorEvent.Udp(
                id = generateRandomId(),
                hostName = hostName,
                port = port,
                throwable = e,
            ),
        )
      }
    }
  }

  @CheckResult
  private fun resolveDestinationInfo(address: SocketAddress): DestinationInfo {
    // Pull out the original packet's address data
    if (address !is InetSocketAddress) {
      throw IllegalArgumentException("UDP Proxy does not handle Unix sockets")
    }

    // Run URL fixers over the hostname url
    val fixedHostName = address.hostname.fixSpecialBuggyUrls()
    val port = address.port

    return DestinationInfo(
        hostName = fixedHostName,
        port = port,
    )
  }

  override suspend fun exchange(data: UdpProxyOptions) {
    Enforcer.assertOffMainThread()

    val packet = data.packet

    // Resolve destination info from original packet
    val destination = resolveDestinationInfo(packet.address)
    val hostName = destination.hostName
    val port = destination.port

    // Craft the send packet from the data
    val sendPacket =
        craftSendPacket(
            packet.packet,
            hostName,
            port,
        )

    sendPacketToDestination(
        data.sender,
        sendPacket,
        hostName,
        port,
    )
  }
}
