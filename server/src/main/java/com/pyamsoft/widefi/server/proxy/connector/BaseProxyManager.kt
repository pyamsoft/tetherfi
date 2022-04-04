package com.pyamsoft.widefi.server.proxy.connector

import androidx.annotation.CheckResult
import com.pyamsoft.pydroid.util.ifNotCancellation
import com.pyamsoft.widefi.server.proxy.SharedProxy
import io.ktor.network.sockets.ASocket
import io.ktor.util.network.NetworkAddress
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
  protected fun getServerAddress(port: Int): NetworkAddress {
    return NetworkAddress(hostname = "0.0.0.0", port = port)
  }

  override suspend fun loop() {
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
