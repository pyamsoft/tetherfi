package com.pyamsoft.tetherfi.server.proxy.session.udp.tracker

import androidx.annotation.CheckResult
import com.pyamsoft.tetherfi.server.ServerInternalApi
import com.pyamsoft.tetherfi.server.proxy.session.DestinationInfo
import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.ConnectedDatagramSocket
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.isClosed
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// Not singleton since this will be provided each time by a Provider<>
internal class UdpConnectionTracker
@Inject
internal constructor(
    @ServerInternalApi private val dispatcher: CoroutineDispatcher,
) : ManagedConnectionTracker {

  private val mutex = Mutex()
  private val connections = mutableMapOf<DestinationInfo, ConnectedDatagramSocket>()

  @CheckResult
  private fun connectToInternet(destination: DestinationInfo): ConnectedDatagramSocket {
    return aSocket(ActorSelectorManager(context = dispatcher))
        .udp()
        .connect(
            remoteAddress =
                InetSocketAddress(
                    hostname = destination.hostName,
                    port = destination.port,
                ),
        )
  }

  override suspend fun use(
      info: DestinationInfo,
      block: suspend (instance: ConnectedDatagramSocket) -> Unit
  ) {
    val connection =
        mutex.withLock {
          val conn: ConnectedDatagramSocket
          val existing = connections[info]
          // The connection has to exist and still be open
          if (existing != null && !existing.isClosed) {
            conn = existing
          } else {
            // Make a new connection
            conn = connectToInternet(info)
            connections[info] = conn
          }

          return@withLock conn
        }

    block(connection)
  }

  override fun close() {
    connections.forEach { entry ->
      val socket = entry.value
      if (!socket.isClosed) {
        socket.dispose()
      }
    }

    connections.clear()
  }

  override fun dispose() {
    try {
      close()
    } catch (ignore: Throwable) {}
  }
}
