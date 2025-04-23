/*
 * Copyright 2024 pyamsoft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pyamsoft.tetherfi.server.proxy.manager

import androidx.annotation.CheckResult
import com.pyamsoft.pydroid.bus.EventConsumer
import com.pyamsoft.pydroid.core.ThreadEnforcer
import com.pyamsoft.pydroid.util.ifNotCancellation
import com.pyamsoft.tetherfi.core.Timber
import com.pyamsoft.tetherfi.server.SocketCreator
import com.pyamsoft.tetherfi.server.event.ServerStopRequestEvent
import com.pyamsoft.tetherfi.server.proxy.ServerDispatcher
import com.pyamsoft.tetherfi.server.proxy.SharedProxy
import com.pyamsoft.tetherfi.server.proxy.SocketTracker
import io.ktor.network.sockets.ASocket
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.SocketAddress
import io.ktor.network.sockets.SocketBuilder
import io.ktor.network.sockets.isClosed
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal abstract class BaseProxyManager<S : ASocket>
protected constructor(
    protected val socketCreator: SocketCreator,
    private val appScope: CoroutineScope,
    private val proxyType: SharedProxy.Type,
    private val enforcer: ThreadEnforcer,
    private val serverStopConsumer: EventConsumer<ServerStopRequestEvent>,
    protected val serverDispatcher: ServerDispatcher,
) : ProxyManager {

  private val logTag: String by lazy { proxyType.name }

  /** Periodically remove any sockets that are alreaedy closed */
  private fun cleanOldSockets(sockets: MutableCollection<ASocket>) {
    enforcer.assertOffMainThread()

    val oldSize = sockets.size
    sockets.removeIf { it.isClosed }
    val newSize = sockets.size

    debugLog { "Clear out old closed sockets Old=${oldSize} New=$newSize" }
  }

  private suspend fun closeAllSockets(
      scope: CoroutineScope,
      mutex: Mutex,
      sockets: MutableCollection<ASocket>
  ) {
    mutex.withLock {
      val waitForClose = mutableSetOf<Deferred<Unit>>()

      for (socket in sockets) {
        if (!socket.isClosed) {
          val job =
              scope.async {
                val start = System.currentTimeMillis()
                socket.dispose()
                val end = System.currentTimeMillis()
                debugLog { "Close socket: $socket (${end - start}ms)" }
              }
          waitForClose.add(job)
        }
      }

      waitForClose.awaitAll()
    }
  }

  /**
   * Close any sockets that are not already closed.
   *
   * Dispose all sockets in parallel to avoid a long wait time
   */
  private suspend fun handleServerClosing(
      scope: CoroutineScope,
      mutex: Mutex,
      sockets: MutableCollection<ASocket>
  ) {
    prepareToDie(
        scope = scope,
        mutex = mutex,
        sockets = sockets,
    )

    mutex.withLock {
      debugLog { "All leftover sockets closed: ${sockets.size}" }
      sockets.clear()
    }
  }

  private fun periodicSocketCleanUp(
      scope: CoroutineScope,
      mutex: Mutex,
      sockets: MutableCollection<ASocket>
  ) {
    scope.launch(context = serverDispatcher.sideEffect) {
      while (isActive) {
        delay(1.minutes)

        mutex.withLock { cleanOldSockets(sockets = sockets) }
      }
    }
  }

  private suspend fun prepareToDie(
      scope: CoroutineScope,
      mutex: Mutex,
      sockets: MutableCollection<ASocket>,
  ) {
    closeAllSockets(
        scope = scope,
        mutex = mutex,
        sockets = sockets,
    )

    onServerClosing()
  }

  private fun listenForStopRequest(
      scope: CoroutineScope,
      mutex: Mutex,
      sockets: MutableCollection<ASocket>,
  ) {
    scope.launch(context = serverDispatcher.sideEffect) {
      serverStopConsumer.also { f ->
        f.collect {
          debugLog { "Received STOP event, prepare to die!" }
          prepareToDie(
              scope = scope,
              mutex = mutex,
              sockets = sockets,
          )
        }
      }
    }
  }

  private fun trackingSideEffects(
      scope: CoroutineScope,
      mutex: Mutex,
      sockets: MutableCollection<ASocket>
  ) {
    periodicSocketCleanUp(
        scope = scope,
        mutex = mutex,
        sockets = sockets,
    )

    listenForStopRequest(
        scope = scope,
        mutex = mutex,
        sockets = sockets,
    )
  }

  /**
   * Try our best to track every single socket we ever make
   *
   * The list is periodically pruned of sockets that are already closed Generally speaking, if we've
   * done everything right this list should always be either empty or composed of closed sockets. We
   * should generally never see the "leftover socket" log message
   */
  private suspend inline fun trackSockets(scope: CoroutineScope, block: (SocketTracker) -> Unit) {
    val mutex = Mutex()
    val closeAllServerSockets = mutableSetOf<ASocket>()

    trackingSideEffects(
        scope = scope,
        mutex = mutex,
        sockets = closeAllServerSockets,
    )

    try {
      block { socket -> mutex.withLock { closeAllServerSockets.add(socket) } }
    } finally {
      // Close leftovers
      handleServerClosing(
          scope = scope,
          mutex = mutex,
          sockets = closeAllServerSockets,
      )
    }
  }

  protected inline fun debugLog(message: () -> String) {
    Timber.d { "$logTag: ${message()}" }
  }

  protected inline fun warnLog(message: () -> String) {
    Timber.w { "$logTag: ${message()}" }
  }

  protected inline fun errorLog(throwable: Throwable, message: () -> String) {
    Timber.e(throwable) { "$logTag: ${message()}" }
  }

  @CheckResult
  protected fun getServerAddress(
      hostName: String,
      port: Int,
      verifyPort: Boolean,
      verifyHostName: Boolean,
  ): SocketAddress {
    // Port must be in the valid range
    if (verifyPort) {
      if (port > 65000) {
        val err = "Port must be <65000: $port"
        warnLog { err }
        throw IllegalArgumentException(err)
      }

      if (port <= 1024) {
        val err = "Port must be >1024: $port"
        warnLog { err }
        throw IllegalArgumentException(err)
      }
    }

    if (verifyHostName) {
      // Name must be valid
      if (hostName.isBlank()) {
        val err = "HostName is invalid: $hostName"
        warnLog { err }
        throw IllegalArgumentException(err)
      }
    }

    return InetSocketAddress(
        hostname = hostName,
        port = port,
    )
  }

  /** This function must ALWAYS call usingSocketBuilder {} or else a socket may potentially leak */
  override suspend fun loop(
      onOpened: suspend () -> Unit,
      onClosing: suspend () -> Unit,
      onError: suspend (Throwable) -> Unit,
  ) =
      withContext(context = serverDispatcher.primary) {
        val scope = this

        try {
          return@withContext socketCreator.create(
              type = SocketCreator.Type.SERVER,
              onError = {
                // This error comes from the SelectorManager launch {} scope,
                // so everything may be dead. fallback to Dispatchers.IO since we cannot be
                // guaranteed that
                // our custom dispatcher pool is around
                appScope.launch(context = Dispatchers.IO) { onError(it) }
              },
              onBuild = { builder ->
                openServer(builder = builder).use { server ->
                  onOpened()

                  // Track the sockets we open so that we can close them later
                  trackSockets(scope = scope) { tracker ->
                    try {
                      runServer(
                          server = server,
                          tracker = tracker,
                      )
                    } catch (e: Throwable) {
                      e.ifNotCancellation {
                        Timber.e(e) { "Error running server" }
                        onError(e)
                      }
                    }
                  }

                  onClosing()
                }
              },
          )
        } catch (e: Throwable) {
          e.ifNotCancellation {
            Timber.e(e) { "Error occurred while opening server" }
            onError(e)
          }
        }
      }

  protected abstract suspend fun runServer(
      tracker: SocketTracker,
      server: S,
  )

  @CheckResult protected abstract suspend fun openServer(builder: SocketBuilder): S

  protected abstract suspend fun onServerClosing()
}
