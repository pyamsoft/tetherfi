package com.pyamsoft.widefi.server.proxy.connector

import androidx.annotation.CheckResult
import com.pyamsoft.pydroid.util.ifNotCancellation
import com.pyamsoft.widefi.server.proxy.SharedProxy
import com.pyamsoft.widefi.server.proxy.session.ProxySession
import com.pyamsoft.widefi.server.proxy.tagSocket
import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.BoundDatagramSocket
import io.ktor.network.sockets.Datagram
import io.ktor.network.sockets.aSocket
import io.ktor.util.network.hostname
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

internal class UdpProxyManager
internal constructor(
    private val port: Int,
    private val dispatcher: CoroutineDispatcher,
    private val factory: ProxySession.Factory<Datagram>,
    proxyDebug: Boolean,
) :
    BaseProxyManager<BoundDatagramSocket, Datagram>(
        SharedProxy.Type.UDP,
        dispatcher,
        proxyDebug,
    ) {

  private val mutex = Mutex()
  private val clientMap = mutableMapOf<String, Boolean>()

  override fun openServer(): BoundDatagramSocket {
    return aSocket(ActorSelectorManager(context = dispatcher))
        .udp()
        .bind(
            localAddress =
                getServerAddress(
                    port = port,
                ),
        )
  }

  override suspend fun acceptClient(server: BoundDatagramSocket): Datagram {
    return server.receive().apply {
      val id = getClientId(this)
      mutex.withLock { clientMap[id] = true }
    }
  }

  override suspend fun runSession(client: Datagram) {
    val session = factory.create()
    try {
      session.exchange(client)
    } catch (e: Throwable) {
      e.ifNotCancellation { Timber.e(e, "${proxyType.name} Error during session") }
    }
  }

  override suspend fun closeSession(client: Datagram) {
    val id = getClientId(client)
    mutex.withLock { clientMap[id] = false }
  }

  companion object {

    @JvmStatic
    @CheckResult
    private fun getClientId(client: Datagram): String {
      return client.address.hostname
    }
  }
}
