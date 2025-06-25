/*
 * Copyright 2025 pyamsoft
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
import com.pyamsoft.tetherfi.server.ExpertPreferences
import com.pyamsoft.tetherfi.server.ProxyPreferences
import com.pyamsoft.tetherfi.server.ServerInternalApi
import com.pyamsoft.tetherfi.server.SocketCreator
import com.pyamsoft.tetherfi.server.broadcast.BroadcastNetworkStatus
import com.pyamsoft.tetherfi.server.event.ServerStopRequestEvent
import com.pyamsoft.tetherfi.server.network.SocketBinder
import com.pyamsoft.tetherfi.server.proxy.ServerDispatcher
import com.pyamsoft.tetherfi.server.proxy.SharedProxy
import com.pyamsoft.tetherfi.server.proxy.SocketTagger
import com.pyamsoft.tetherfi.server.proxy.manager.ProxyManager
import com.pyamsoft.tetherfi.server.proxy.manager.TcpProxyManager
import com.pyamsoft.tetherfi.server.proxy.session.ProxySession
import com.pyamsoft.tetherfi.server.proxy.session.tcp.TcpProxyData
import javax.inject.Inject
import javax.inject.Named
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

internal class DefaultProxyManagerFactory
@Inject
internal constructor(
    @ServerInternalApi private val socketBinder: SocketBinder,
    @Named("app_scope") private val appScope: CoroutineScope,
    @Named("http") private val httpSession: ProxySession<TcpProxyData>,
    @Named("socks") private val socksSession: ProxySession<TcpProxyData>,
    private val expertPreferences: ExpertPreferences,
    private val socketTagger: SocketTagger,
    private val enforcer: ThreadEnforcer,
    private val proxyPreferences: ProxyPreferences,
    private val appEnvironment: AppDevEnvironment,
    private val serverStopConsumer: EventConsumer<ServerStopRequestEvent>,
) : ProxyManager.Factory {

  @CheckResult
  private fun createTcp(
      proxyType: SharedProxy.Type,
      session: ProxySession<TcpProxyData>,
      info: BroadcastNetworkStatus.ConnectionInfo.Connected,
      socketCreator: SocketCreator,
      dispatcher: ServerDispatcher,
      port: Int,
  ): ProxyManager {
    enforcer.assertOffMainThread()

    return TcpProxyManager(
        appScope = appScope,
        socketTagger = socketTagger,
        appEnvironment = appEnvironment,
        yoloRepeatDelay = 3.seconds,
        enforcer = enforcer,
        serverStopConsumer = serverStopConsumer,
        socketBinder = socketBinder,
        expertPreferences = expertPreferences,
        proxyType = proxyType,
        session = session,
        hostConnection = info,
        port = port,
        serverDispatcher = dispatcher,
        socketCreator = socketCreator,
    )
  }

  @CheckResult
  private suspend fun createHttp(
      info: BroadcastNetworkStatus.ConnectionInfo.Connected,
      socketCreator: SocketCreator,
      dispatcher: ServerDispatcher,
  ): ProxyManager {
    enforcer.assertOffMainThread()

    val port = proxyPreferences.listenForHttpPortChanges().first()

    return createTcp(
        proxyType = SharedProxy.Type.HTTP,
        session = httpSession,
        info = info,
        socketCreator = socketCreator,
        dispatcher = dispatcher,
        port = port,
    )
  }

  @CheckResult
  private suspend fun createSocks(
      info: BroadcastNetworkStatus.ConnectionInfo.Connected,
      socketCreator: SocketCreator,
      dispatcher: ServerDispatcher,
  ): ProxyManager {
    enforcer.assertOffMainThread()

    val port = proxyPreferences.listenForSocksPortChanges().first()

    return createTcp(
        proxyType = SharedProxy.Type.SOCKS,
        session = socksSession,
        info = info,
        socketCreator = socketCreator,
        dispatcher = dispatcher,
        port = port,
    )
  }

  override suspend fun create(
      type: SharedProxy.Type,
      info: BroadcastNetworkStatus.ConnectionInfo.Connected,
      socketCreator: SocketCreator,
      serverDispatcher: ServerDispatcher,
  ): ProxyManager =
      withContext(context = Dispatchers.Default) {
        return@withContext when (type) {
          SharedProxy.Type.HTTP ->
              createHttp(
                  info = info,
                  socketCreator = socketCreator,
                  dispatcher = serverDispatcher,
              )
          SharedProxy.Type.SOCKS ->
              createSocks(
                  info = info,
                  socketCreator = socketCreator,
                  dispatcher = serverDispatcher,
              )
        }
      }
}
