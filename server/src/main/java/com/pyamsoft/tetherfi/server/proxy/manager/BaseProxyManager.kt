package com.pyamsoft.tetherfi.server.proxy.manager

import androidx.annotation.CheckResult
import com.pyamsoft.pydroid.core.Enforcer
import com.pyamsoft.pydroid.util.ifNotCancellation
import com.pyamsoft.tetherfi.server.proxy.SharedProxy
import com.pyamsoft.tetherfi.server.proxy.session.tagSocket
import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.ASocket
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.SocketAddress
import io.ktor.network.sockets.SocketBuilder
import io.ktor.network.sockets.aSocket
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

internal abstract class BaseProxyManager<S : ASocket>(
    private val proxyType: SharedProxy.Type,
    private val dispatcher: CoroutineDispatcher,
    private val port: Int,
) : ProxyManager {

  private suspend fun serverLoop(server: S) = coroutineScope {
    Enforcer.assertOffMainThread()
    val scope = this
    while (scope.isActive) {
      try {
        scope.launch(context = dispatcher) {
          Enforcer.assertOffMainThread()
          runServer(server)
        }
      } catch (e: Throwable) {
        e.ifNotCancellation { errorLog(e, "Error running serverLoop") }
      }
    }
  }

  @CheckResult
  private fun getServerAddress(port: Int): SocketAddress {
    return InetSocketAddress(hostname = "0.0.0.0", port = port)
  }

  protected fun errorLog(error: Throwable, message: String) {
    Timber.e(error, "${proxyType.name}: $message")
  }

  protected fun debugLog(message: String) {
    Timber.d("${proxyType.name}: $message")
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
      serverLoop(server)
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
