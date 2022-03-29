package com.pyamsoft.widefi.server.proxy.connector

import androidx.annotation.CheckResult
import com.pyamsoft.pydroid.util.ifNotCancellation
import com.pyamsoft.widefi.server.proxy.SharedProxy
import com.pyamsoft.widefi.server.status.RunningStatus
import com.pyamsoft.widefi.server.status.StatusBroadcast
import io.ktor.network.sockets.ASocket
import io.ktor.util.network.NetworkAddress
import java.io.Closeable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

internal abstract class BaseProxyConnection<SS : ASocket, CS : Any>(
    protected val proxyType: SharedProxy.Type,
    private val status: StatusBroadcast,
    private val dispatcher: CoroutineDispatcher,
    protected val proxyDebug: Boolean,
) {

  /** Log only when session is in debug mode */
  protected inline fun debugLog(message: () -> String) {
    if (proxyDebug) {
      Timber.d(message())
    }
  }

  /** As long as the socket is alive, we loop the connection and accept new connections */
  private suspend inline fun whileSocketAlive(
      socket: SS,
      crossinline block: suspend (CS) -> Unit,
  ) = coroutineScope {
    while (isActive) {
      debugLog { "Awaiting new ${proxyType.name} socket connection..." }
      val s = acceptClientSocket(socket)

      launch(context = dispatcher) {
        if (s is Closeable) {
          s.use { block(it) }
        } else {
          block(s)
        }
      }
    }
  }

  @CheckResult
  protected fun getServerAddress(port: Int): NetworkAddress {
    return NetworkAddress(hostname = "0.0.0.0", port = port)
  }

  suspend fun loop() {
    val socket = openServerSocket()
    status.set(RunningStatus.Running)

    socket.use { ss ->
      whileSocketAlive(ss) { s ->
        try {
          handleSocketSession(s)
        } catch (e: Throwable) {
          e.ifNotCancellation {
            Timber.e(e, "Error during ${proxyType.name} session communication")
          }
        }
      }
    }
  }

  @CheckResult protected abstract suspend fun handleSocketSession(client: CS)

  @CheckResult protected abstract suspend fun acceptClientSocket(server: SS): CS

  @CheckResult protected abstract fun openServerSocket(): SS
}
