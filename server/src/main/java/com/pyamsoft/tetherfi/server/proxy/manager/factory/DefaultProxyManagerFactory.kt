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

package com.pyamsoft.tetherfi.server.proxy.manager.factory

import androidx.annotation.CheckResult
import com.pyamsoft.pydroid.bus.EventConsumer
import com.pyamsoft.pydroid.core.ThreadEnforcer
import com.pyamsoft.tetherfi.core.AppDevEnvironment
import com.pyamsoft.tetherfi.server.ProxyPreferences
import com.pyamsoft.tetherfi.server.ServerInternalApi
import com.pyamsoft.tetherfi.server.broadcast.BroadcastNetworkStatus
import com.pyamsoft.tetherfi.server.event.ServerStopRequestEvent
import com.pyamsoft.tetherfi.server.network.SocketBinder
import com.pyamsoft.tetherfi.server.proxy.ServerDispatcher
import com.pyamsoft.tetherfi.server.proxy.SharedProxy
import com.pyamsoft.tetherfi.server.proxy.SocketTagger
import com.pyamsoft.tetherfi.server.proxy.manager.ProxyManager
import com.pyamsoft.tetherfi.server.proxy.manager.TcpProxyManager
import com.pyamsoft.tetherfi.server.proxy.session.ProxySession
import com.pyamsoft.tetherfi.server.proxy.session.tcp.ProxyRequest
import com.pyamsoft.tetherfi.server.proxy.session.tcp.TcpProxyData
import com.pyamsoft.tetherfi.server.proxy.session.tcp.TcpSessionTransport
import com.pyamsoft.tetherfi.server.proxy.session.tcp.http.HttpProxyRequest
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

internal class DefaultProxyManagerFactory
@Inject
internal constructor(
    @ServerInternalApi private val socketBinder: SocketBinder,
    @ServerInternalApi private val httpTransport: TcpSessionTransport<HttpProxyRequest>,
    @ServerInternalApi private val httpSession: ProxySession<TcpProxyData>,
    private val socketTagger: SocketTagger,
    private val enforcer: ThreadEnforcer,
    private val preferences: ProxyPreferences,
    private val appEnvironment: AppDevEnvironment,
    private val serverStopConsumer: EventConsumer<ServerStopRequestEvent>,
) : ProxyManager.Factory {

  @CheckResult
  private fun <Q : ProxyRequest> createTcp(
      session: ProxySession<TcpProxyData>,
      transport: TcpSessionTransport<Q>,
      info: BroadcastNetworkStatus.ConnectionInfo.Connected,
      dispatcher: ServerDispatcher,
      port: Int,
  ): ProxyManager {
    enforcer.assertOffMainThread()

    return TcpProxyManager(
        socketTagger = socketTagger,
        appEnvironment = appEnvironment,
        yoloRepeatDelay = 3.seconds,
        enforcer = enforcer,
        serverStopConsumer = serverStopConsumer,
        socketBinder = socketBinder,
        session = session,
        hostConnection = info,
        port = port,
        serverDispatcher = dispatcher,
        transport = transport,
    )
  }

  @CheckResult
  private suspend fun createHttp(
      info: BroadcastNetworkStatus.ConnectionInfo.Connected,
      dispatcher: ServerDispatcher,
  ): ProxyManager {
    enforcer.assertOffMainThread()

    val port = preferences.listenForPortChanges().first()

    return createTcp(
        transport = httpTransport,
        session = httpSession,
        info = info,
        dispatcher = dispatcher,
        port = port,
    )
  }

  override suspend fun create(
      type: SharedProxy.Type,
      info: BroadcastNetworkStatus.ConnectionInfo.Connected,
      serverDispatcher: ServerDispatcher,
  ): ProxyManager =
      withContext(context = Dispatchers.Default) {
        return@withContext when (type) {
          SharedProxy.Type.HTTP ->
              createHttp(
                  info = info,
                  dispatcher = serverDispatcher,
              )
          SharedProxy.Type.SOCKS ->
              throw IllegalArgumentException("Proxy type $type not supported yet!")
        }
      }
}
