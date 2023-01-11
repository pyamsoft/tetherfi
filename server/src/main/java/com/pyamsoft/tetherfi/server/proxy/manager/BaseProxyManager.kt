package com.pyamsoft.tetherfi.server.proxy.manager

import androidx.annotation.CheckResult
import com.pyamsoft.pydroid.core.Enforcer
import com.pyamsoft.tetherfi.server.ProxyDebug
import com.pyamsoft.tetherfi.server.proxy.SharedProxy
import com.pyamsoft.tetherfi.server.proxy.session.tagSocket
import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.ASocket
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.SocketAddress
import io.ktor.network.sockets.SocketBuilder
import io.ktor.network.sockets.aSocket
import kotlinx.coroutines.CoroutineDispatcher
import timber.log.Timber

internal abstract class BaseProxyManager<S : ASocket>(
    private val proxyType: SharedProxy.Type,
    private val dispatcher: CoroutineDispatcher,
    private val port: Int,
    private val proxyDebug: ProxyDebug,
) : ProxyManager {

  @CheckResult
  private fun getServerAddress(port: Int): SocketAddress {
    return InetSocketAddress(hostname = "0.0.0.0", port = port)
  }

  /** Log only when session is in debug mode */
  protected inline fun debugLog(message: () -> String) {
    if (proxyDebug.isAllowed(proxyType)) {
      Timber.d("${proxyType.name}: ${message()}")
    }
  }

  /** Log only when session is in debug mode */
  protected inline fun warnLog(message: () -> String) {
    if (proxyDebug.isAllowed(proxyType)) {
      Timber.w("${proxyType.name}: ${message()}")
    }
  }

  /** Log only when session is in debug mode */
  protected inline fun errorLog(throwable: Throwable, message: () -> String) {
    if (proxyDebug.isAllowed(proxyType)) {
      Timber.e(throwable, "${proxyType.name}: ${message()}")
    }
  }

  override suspend fun loop() {
    Enforcer.assertOffMainThread()

    // Tag sockets for Android O strict mode
    tagSocket()

    val server =
        openServer(
            builder = aSocket(ActorSelectorManager(context = dispatcher)),
            localAddress = getServerAddress(port = port),
        )
    try {
      runServer(server)
    } finally {
      server.dispose()
      onServerClosed()
    }
  }

  protected abstract suspend fun runServer(server: S)

  @CheckResult
  protected abstract suspend fun openServer(
      builder: SocketBuilder,
      localAddress: SocketAddress,
  ): S

  @CheckResult protected abstract suspend fun onServerClosed()
}
