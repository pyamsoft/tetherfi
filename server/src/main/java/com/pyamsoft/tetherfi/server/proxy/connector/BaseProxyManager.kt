package com.pyamsoft.tetherfi.server.proxy.connector

import androidx.annotation.CheckResult
import com.pyamsoft.pydroid.util.ifNotCancellation
import com.pyamsoft.tetherfi.server.proxy.SharedProxy
import com.pyamsoft.tetherfi.server.proxy.tagSocket
import io.ktor.network.sockets.ASocket
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.SocketAddress
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

internal abstract class BaseProxyManager<SS : ASocket, CS : Any>(
    protected val proxyType: SharedProxy.Type,
    private val dispatcher: CoroutineDispatcher,
    protected val proxyDebug: Boolean,
) : ProxyManager {

  /** Log only when session is in debug mode */
  protected inline fun debugLog(message: () -> String) {
    if (proxyDebug) {
      Timber.d(message())
    }
  }

  /** As long as the socket is alive, we loop the connection and accept new connections */
  private suspend inline fun whileServerAlive(
      server: SS,
      crossinline block: suspend (CS) -> Unit,
  ) = coroutineScope {
    while (isActive) {
      debugLog { "Awaiting new ${proxyType.name} socket connection..." }
      val s = acceptClient(server)

      launch(context = dispatcher) { block(s) }
    }
  }

  @CheckResult
  protected fun getServerAddress(port: Int): SocketAddress {
    return InetSocketAddress(hostname = "0.0.0.0", port = port)
  }

  override suspend fun loop() {
    // Tag sockets for Android O strict mode
    tagSocket()

    val server = openServer()

    try {
      whileServerAlive(server) { s ->
        try {
          runSession(s)
        } catch (e: Throwable) {
          e.ifNotCancellation {
            Timber.e(e, "Error during ${proxyType.name} session communication")
          }
        } finally {
          closeSession(s)
        }
      }
    } finally {
      // Close socket
      server.dispose()
    }
  }

  protected abstract suspend fun closeSession(client: CS)

  @CheckResult protected abstract suspend fun runSession(client: CS)

  @CheckResult protected abstract suspend fun acceptClient(server: SS): CS

  @CheckResult protected abstract fun openServer(): SS
}
