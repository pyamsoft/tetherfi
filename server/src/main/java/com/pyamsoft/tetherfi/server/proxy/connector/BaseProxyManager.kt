package com.pyamsoft.tetherfi.server.proxy.connector

import androidx.annotation.CheckResult
import com.pyamsoft.pydroid.core.Enforcer
import com.pyamsoft.pydroid.util.ifNotCancellation
import com.pyamsoft.tetherfi.server.proxy.SharedProxy
import com.pyamsoft.tetherfi.server.proxy.tagSocket
import io.ktor.network.sockets.ASocket
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.SocketAddress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import timber.log.Timber

internal abstract class BaseProxyManager<S : ASocket>(
    protected val proxyType: SharedProxy.Type,
    protected val proxyDebug: Boolean,
) : ProxyManager {

  private suspend fun serverLoop(server: S) = coroutineScope {
    while (isActive) {
      debugLog { "Awaiting new ${proxyType.name} socket connection..." }

      try {
        newSession(server)
      } catch (e: Throwable) {
        e.ifNotCancellation { Timber.e(e, "Error during ${proxyType.name} session") }
      }
    }
  }

  /** Log only when session is in debug mode */
  protected inline fun debugLog(message: () -> String) {
    if (proxyDebug) {
      Timber.d(message())
    }
  }

  @CheckResult
  protected fun getServerAddress(port: Int): SocketAddress {
    return InetSocketAddress(hostname = "0.0.0.0", port = port)
  }

  override suspend fun loop() {
    Enforcer.assertOffMainThread()

    // Tag sockets for Android O strict mode
    tagSocket()

    val server = openServer()
    try {
      serverLoop(server)
    } finally {
      server.dispose()
      onServerClosed()
    }
  }

  @CheckResult protected abstract suspend fun CoroutineScope.newSession(server: S)

  @CheckResult protected abstract fun openServer(): S

  @CheckResult protected abstract fun onServerClosed()
}
