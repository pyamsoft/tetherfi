package com.pyamsoft.tetherfi.server.proxy.manager

import androidx.annotation.CheckResult
import com.pyamsoft.pydroid.core.ThreadEnforcer
import com.pyamsoft.tetherfi.server.ProxyDebug
import com.pyamsoft.tetherfi.server.proxy.ProxyLogger
import com.pyamsoft.tetherfi.server.proxy.SharedProxy
import com.pyamsoft.tetherfi.server.proxy.session.tagSocket
import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.ASocket
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.SocketAddress
import io.ktor.network.sockets.SocketBuilder
import io.ktor.network.sockets.aSocket
import kotlin.coroutines.CoroutineContext

internal abstract class BaseProxyManager<S : ASocket>(
    proxyType: SharedProxy.Type,
    proxyDebug: ProxyDebug,
    private val enforcer: ThreadEnforcer,
) :
    ProxyManager,
    ProxyLogger(
        proxyType,
        proxyDebug,
    ) {

  @CheckResult
  private fun getServerAddress(port: Int): SocketAddress {
    return InetSocketAddress(hostname = "0.0.0.0", port = port)
  }

  override suspend fun loop(
      context: CoroutineContext,
      port: Int,
  ) {
    enforcer.assertOffMainThread()

    // Tag sockets for Android O strict mode
    tagSocket()

    val server =
        openServer(
            builder = aSocket(ActorSelectorManager(context = context)),
            localAddress = getServerAddress(port = port),
        )
    try {
      runServer(context, server)
    } finally {
      server.dispose()
      onServerClosed()
    }
  }

  protected abstract suspend fun runServer(
      context: CoroutineContext,
      server: S,
  )

  @CheckResult
  protected abstract suspend fun openServer(
      builder: SocketBuilder,
      localAddress: SocketAddress,
  ): S

  @CheckResult protected abstract suspend fun onServerClosed()
}
