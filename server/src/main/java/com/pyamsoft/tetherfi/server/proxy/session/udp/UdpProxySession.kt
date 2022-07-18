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
import com.pyamsoft.tetherfi.server.proxy.session.UrlFixer
import com.pyamsoft.tetherfi.server.proxy.session.data.UdpProxyData
import io.ktor.network.sockets.BoundDatagramSocket
import io.ktor.network.sockets.ConnectedDatagramSocket
import io.ktor.network.sockets.Datagram
import io.ktor.network.sockets.DatagramWriteChannel
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.SocketAddress
import io.ktor.network.sockets.isClosed
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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

  private suspend fun exchangeInternet(
      internet: ConnectedDatagramSocket,
      proxy: BoundDatagramSocket,
      destination: DestinationInfo,
  ) = coroutineScope {
    try {
      val job =
          launch(context = dispatcher) {
            // Loop as long as the job is alive and the connections are alive
            while (isActive && !internet.isClosed && !proxy.isClosed) {
              // Receive datagrams from the internet
              val datagram = internet.receive()

              // Send them back to the proxy
              proxy.send(datagram)
            }
          }

      // Wait for internet communication to finish
      job.join()
      debugLog { "Done with proxy request: $destination" }
    } catch (e: Throwable) {
      e.ifNotCancellation {
        errorLog(e) { "Error during Internet exchange" }
        errorBus.send(
            ErrorEvent.Udp(
                id = generateRandomId(),
                throwable = e,
                destination = destination,
            ),
        )
      }
    }
  }

  @CheckResult
  private suspend fun sendInitialPacket(
      internet: DatagramWriteChannel,
      packet: Datagram,
      destination: DestinationInfo,
  ): Boolean {
    debugLog { "Forward datagram to destination: $destination" }

    // Log connection
    connectionBus.send(
        ConnectionEvent.Udp(
            id = generateRandomId(),
            destination = destination,
        ),
    )

    // Send the data to the socket
    try {
      internet.send(packet)
      return true
    } catch (e: Throwable) {
      e.ifNotCancellation {
        errorLog(e) { "Error during datagram forwarding: $destination" }
        errorBus.send(
            ErrorEvent.Udp(
                id = generateRandomId(),
                throwable = e,
                destination = destination,
            ),
        )
      }
      return false
    }
  }

  override suspend fun exchange(data: UdpProxyData) {
    Enforcer.assertOffMainThread()

    val runtime = data.runtime
    val environment = data.environment

    val packet = runtime.initialPacket

    // Resolve destination info from original packet
    val destination = resolveDestinationInfo(packet.address)

    val connections = environment.connectionProducer
    connections.use(destination) { internet ->
      // Send the initial packet
      val isInitialSendSuccess =
          sendInitialPacket(
              internet = internet,
              packet = packet,
              destination = destination,
          )

      if (isInitialSendSuccess) {
        // Open a connection and wait for packets back from the internet destination
        exchangeInternet(
            internet = internet,
            proxy = runtime.proxy,
            destination = destination,
        )
      }
    }
  }
}
