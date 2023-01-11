package com.pyamsoft.tetherfi.server.proxy.manager

import androidx.annotation.CheckResult
import com.pyamsoft.pydroid.util.ifNotCancellation
import com.pyamsoft.tetherfi.server.ServerInternalApi
import com.pyamsoft.tetherfi.server.proxy.SharedProxy
import com.pyamsoft.tetherfi.server.urlfixer.UrlFixer
import io.ktor.network.sockets.BoundDatagramSocket
import io.ktor.network.sockets.SocketAddress
import io.ktor.network.sockets.SocketBuilder
import io.ktor.network.sockets.isClosed
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

internal class UdpProxyManager
internal constructor(
    port: Int,
    /** Need to use MutableSet instead of Set because of Java -> Kotlin fun. */
    @ServerInternalApi private val urlFixers: MutableSet<UrlFixer>,
    private val dispatcher: CoroutineDispatcher,
) :
    BaseProxyManager<BoundDatagramSocket>(
        SharedProxy.Type.UDP,
        dispatcher,
        port,
    ) {

  /**
   * Some connection request formats are buggy, this method seeks to fix them to what it knows in
   * very specific cases is correct
   */
  @CheckResult
  private fun String.fixSpecialBuggyUrls(): String {
    var result = this
    for (fixer in urlFixers) {
      result = fixer.fix(result)
    }
    return result
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

  override suspend fun runServer(server: BoundDatagramSocket) = coroutineScope {
    try {
      while (!server.isClosed && isActive) {
        val packet = server.receive()
        launch(context = dispatcher) { Timber.d("Received UDP packet: $packet ${packet.address}") }
      }
    } catch (e: Throwable) {
      e.ifNotCancellation { errorLog(e, "Error during runServer") }
    }
  }

  override suspend fun onServerClosed() {}
}
