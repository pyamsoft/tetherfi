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

package com.pyamsoft.tetherfi.server

import com.pyamsoft.pydroid.bus.internal.DefaultEventBus
import com.pyamsoft.pydroid.core.ThreadEnforcer
import com.pyamsoft.tetherfi.core.AppDevEnvironment
import com.pyamsoft.tetherfi.server.broadcast.BroadcastNetworkStatus
import com.pyamsoft.tetherfi.server.clients.AllowedClients
import com.pyamsoft.tetherfi.server.clients.BlockedClients
import com.pyamsoft.tetherfi.server.clients.ByteTransferReport
import com.pyamsoft.tetherfi.server.clients.ClientResolver
import com.pyamsoft.tetherfi.server.clients.TetherClient
import com.pyamsoft.tetherfi.server.network.PassthroughSocketBinder
import com.pyamsoft.tetherfi.server.proxy.ServerDispatcher
import com.pyamsoft.tetherfi.server.proxy.SocketTagger
import com.pyamsoft.tetherfi.server.proxy.manager.TcpProxyManager
import com.pyamsoft.tetherfi.server.proxy.session.tcp.http.HttpProxySession
import com.pyamsoft.tetherfi.server.proxy.session.tcp.http.HttpTransport
import com.pyamsoft.tetherfi.server.proxy.session.tcp.http.UrlRequestParser
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.newSingleThreadContext
import timber.log.Timber
import java.io.IOException
import java.time.Clock
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
internal suspend inline fun setupProxy(
    scope: CoroutineScope,
    isLoggingEnabled: Boolean = false,
    expectServerFail: Boolean = false,
    appEnv: AppDevEnvironment.() -> Unit = {},
    withServer: CoroutineScope.(CoroutineDispatcher) -> Unit,
) {
    val dispatcher =
        object : ServerDispatcher {
            override val primary = newSingleThreadContext("TEST")
            override val sideEffect = primary

            override fun shutdown() {}
        }

    val enforcer =
        object : ThreadEnforcer {
            override fun assertOffMainThread() {}

            override fun assertOnMainThread() {}
        }

    val blocked =
        object : BlockedClients {
            override fun listenForBlocked(): Flow<Collection<TetherClient>> {
                return flowOf(emptyList())
            }

            override fun isBlocked(client: TetherClient): Boolean {
                return false
            }
        }

    val allowed =
        object : AllowedClients {
            override fun listenForClients(): Flow<List<TetherClient>> {
                return flowOf(emptyList())
            }

            override suspend fun seen(client: TetherClient) {}

            override suspend fun reportTransfer(client: TetherClient, report: ByteTransferReport) {}
        }

    val resolver =
        object : ClientResolver {

            private val clients = mutableMapOf<String, TetherClient>()

            override fun ensure(hostNameOrIp: String): TetherClient {
                return clients.getOrPut(hostNameOrIp) {
                    TetherClient.create(
                        hostNameOrIp,
                        clock = Clock.systemDefaultZone(),
                    )
                }
            }
        }

    val socketTagger = SocketTagger {}

    if (isLoggingEnabled) {
        Timber.plant(
            object : Timber.Tree() {
                override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                    t?.printStackTrace()
                    println(message)
                }
            })
    }

    val transport =
        HttpTransport(
            requestParser =
            UrlRequestParser(
                urlFixers = mutableSetOf(),
            ),
            enforcer = enforcer,
        )

    val manager =
        TcpProxyManager(
            appEnvironment = AppDevEnvironment().apply(appEnv),
            session =
            HttpProxySession(
                transport = transport,
                blockedClients = blocked,
                allowedClients = allowed,
                enforcer = enforcer,
                socketTagger = socketTagger,
                clientResolver = resolver,
            ),
            hostConnection =
            BroadcastNetworkStatus.ConnectionInfo.Connected(
                hostName = HOSTNAME,
            ),
            port = PROXY_PORT,
            serverDispatcher = dispatcher,
            socketTagger = socketTagger,
            yoloRepeatDelay = 0.seconds,
            enforcer = enforcer,
            serverStopConsumer = DefaultEventBus(),
            socketBinder = PassthroughSocketBinder(),
        )

    val server =
        scope.async {
            val block = suspend {
                manager.loop(
                    onOpened = {},
                    onClosing = {},
                )
            }

            if (expectServerFail) {
                assertFailsWith<IOException> { block() }
            } else {
                block()
            }
        }

    println("Start TetherFi proxy $HOSTNAME $PROXY_PORT")
    delay(3.seconds)

    println("Run with TetherFi proxy")
    scope.withServer(dispatcher.primary)

    println("Done TetherFi proxy")
    server.cancel()
}
