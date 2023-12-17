/*
 * Copyright 2023 pyamsoft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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
import com.pyamsoft.pydroid.core.ThreadEnforcer
import com.pyamsoft.tetherfi.server.ConfigPreferences
import com.pyamsoft.tetherfi.server.ServerInternalApi
import com.pyamsoft.tetherfi.server.ServerPreferences
import com.pyamsoft.tetherfi.server.broadcast.BroadcastNetworkStatus
import com.pyamsoft.tetherfi.server.proxy.ServerDispatcher
import com.pyamsoft.tetherfi.server.proxy.SharedProxy
import com.pyamsoft.tetherfi.server.proxy.manager.ProxyManager
import com.pyamsoft.tetherfi.server.proxy.manager.TcpProxyManager
import com.pyamsoft.tetherfi.server.proxy.manager.UdpProxyManager
import com.pyamsoft.tetherfi.server.proxy.session.ProxySession
import com.pyamsoft.tetherfi.server.proxy.session.tcp.TcpProxyData
import com.pyamsoft.tetherfi.server.proxy.session.udp.UdpProxyData
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

internal class DefaultProxyManagerFactory
@Inject
internal constructor(
    @ServerInternalApi private val tcpSession: ProxySession<TcpProxyData>,
    @ServerInternalApi private val udpSession: ProxySession<UdpProxyData>,
    private val enforcer: ThreadEnforcer,
    private val serverPreferences: ServerPreferences,
    private val configPreferences: ConfigPreferences,
) : ProxyManager.Factory {

  @CheckResult
  private suspend fun createTcp(
      info: BroadcastNetworkStatus.ConnectionInfo.Connected,
      dispatcher: ServerDispatcher,
  ): ProxyManager {
    enforcer.assertOffMainThread()

    val port = configPreferences.listenForPortChanges().first()

    return TcpProxyManager(
        preferences = serverPreferences,
        enforcer = enforcer,
        session = tcpSession,
        hostName = info.hostName,
        port = port,
        serverDispatcher = dispatcher,
    )
  }

  @CheckResult
  private fun createUdp(
      info: BroadcastNetworkStatus.ConnectionInfo.Connected,
      dispatcher: ServerDispatcher,
  ): ProxyManager {
    enforcer.assertOffMainThread()

    return UdpProxyManager(
        preferences = serverPreferences,
        enforcer = enforcer,
        session = udpSession,
        hostName = info.hostName,
        serverDispatcher = dispatcher,
    )
  }

  override suspend fun create(
      type: SharedProxy.Type,
      info: BroadcastNetworkStatus.ConnectionInfo.Connected,
      serverDispatcher: ServerDispatcher,
  ): ProxyManager =
      withContext(context = Dispatchers.Default) {
        return@withContext when (type) {
          SharedProxy.Type.TCP ->
              createTcp(
                  info = info,
                  dispatcher = serverDispatcher,
              )
          SharedProxy.Type.UDP ->
              createUdp(
                  info = info,
                  dispatcher = serverDispatcher,
              )
        }
      }
}
