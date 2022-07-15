package com.pyamsoft.tetherfi.server.proxy.session.udp

import androidx.annotation.CheckResult
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.core.Enforcer
import com.pyamsoft.pydroid.util.ifNotCancellation
import com.pyamsoft.tetherfi.core.generateRandomId
import com.pyamsoft.tetherfi.server.ServerInternalApi
import com.pyamsoft.tetherfi.server.event.ConnectionEvent
import com.pyamsoft.tetherfi.server.event.ErrorEvent
import com.pyamsoft.tetherfi.server.proxy.SharedProxy
import com.pyamsoft.tetherfi.server.proxy.session.BaseProxySession
import com.pyamsoft.tetherfi.server.proxy.session.DestinationInfo
import com.pyamsoft.tetherfi.server.proxy.session.data.UdpProxyData
import com.pyamsoft.tetherfi.server.proxy.session.urlfixer.UrlFixer
import io.ktor.network.sockets.Datagram
import io.ktor.network.sockets.DatagramWriteChannel
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.SocketAddress
import io.ktor.utils.io.core.ByteReadPacket
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher

@Singleton
internal class UdpProxySession
@Inject
internal constructor(
    // Need to use MutableSet instead of Set because of Java -> Kotlin fun.
    @ServerInternalApi urlFixers: MutableSet<UrlFixer>,
    @ServerInternalApi proxyDebug: Boolean,
    @ServerInternalApi private val dispatcher: CoroutineDispatcher,
    @ServerInternalApi private val errorBus: EventBus<ErrorEvent>,
    @ServerInternalApi private val connectionBus: EventBus<ConnectionEvent>,
) :
    BaseProxySession<UdpProxyData>(
        SharedProxy.Type.UDP,
        urlFixers,
        proxyDebug,
    ) {

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
      sender: DatagramWriteChannel,
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

  override suspend fun exchange(data: UdpProxyData) {
    Enforcer.assertOffMainThread()

    val packet = data.initialPacket

    // Resolve destination info from original packet
    val destination = resolveDestinationInfo(packet.address)
  }

  override suspend fun finish() {}
}
