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
import com.pyamsoft.tetherfi.core.Timber
import com.pyamsoft.tetherfi.server.event.ServerStopRequestEvent
import com.pyamsoft.tetherfi.server.proxy.ServerDispatcher
import com.pyamsoft.tetherfi.server.proxy.SocketTracker
import com.pyamsoft.tetherfi.server.proxy.usingSocketBuilder
import io.ktor.network.sockets.ASocket
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.SocketAddress
import io.ktor.network.sockets.SocketBuilder
import io.ktor.network.sockets.isClosed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.minutes

internal abstract class BaseProxyManager<S : ASocket>
protected constructor(
    private val enforcer: ThreadEnforcer,
    private val serverStopConsumer: EventConsumer<ServerStopRequestEvent>,
    protected val serverDispatcher: ServerDispatcher,
) : ProxyManager {

    /** Periodically remove any sockets that are alreaedy closed */
    private fun cleanOldSockets(sockets: MutableCollection<ASocket>) {
        enforcer.assertOffMainThread()

        val oldSize = sockets.size
        sockets.removeIf { it.isClosed }
        val newSize = sockets.size

        Timber.d { "Clear out old closed sockets Old=${oldSize} New=$newSize" }
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
                            Timber.d { "Close socket: $socket (${end - start}ms)" }
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
            Timber.d { "All leftover sockets closed: ${sockets.size}" }
            sockets.clear()
        }
    }

    private suspend fun periodicSocketCleanUp(
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

    private suspend fun listenForStopRequest(
        scope: CoroutineScope,
        mutex: Mutex,
        sockets: MutableCollection<ASocket>,
    ) {
        scope.launch(context = serverDispatcher.sideEffect) {
            serverStopConsumer.also { f ->
                f.collect {
                    Timber.d { "Received STOP event, prepare to die!" }
                    prepareToDie(
                        scope = scope,
                        mutex = mutex,
                        sockets = sockets,
                    )
                }
            }
        }
    }

    private suspend fun trackingSideEffects(
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
                Timber.w { err }
                throw IllegalArgumentException(err)
            }

            if (port <= 1024) {
                val err = "Port must be >1024: $port"
                Timber.w { err }
                throw IllegalArgumentException(err)
            }
        }

        if (verifyHostName) {
            // Name must be valid
            if (hostName.isBlank()) {
                val err = "HostName is invalid: $hostName"
                Timber.w { err }
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
        onOpened: () -> Unit,
        onClosing: () -> Unit,
    ) =
        withContext(context = serverDispatcher.primary) {
            val scope = this

            return@withContext usingSocketBuilder(serverDispatcher.primary) { builder ->
                openServer(builder = builder).use { server ->
                    onOpened()

                    // Track the sockets we open so that we can close them later
                    trackSockets(scope = scope) { tracker ->
                        runServer(
                            server = server,
                            tracker = tracker,
                        )
                    }

                    onClosing()
                }
            }
        }

    protected abstract suspend fun runServer(
        tracker: SocketTracker,
        server: S,
    )

    @CheckResult
    protected abstract suspend fun openServer(builder: SocketBuilder): S

    protected abstract suspend fun onServerClosing()
}
