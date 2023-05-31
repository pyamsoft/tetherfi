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
import com.pyamsoft.tetherfi.server.ServerInternalApi
import com.pyamsoft.tetherfi.server.proxy.SharedProxy
import com.pyamsoft.tetherfi.server.proxy.manager.ProxyManager
import com.pyamsoft.tetherfi.server.proxy.manager.TcpProxyManager
import com.pyamsoft.tetherfi.server.proxy.session.ProxySession
import com.pyamsoft.tetherfi.server.proxy.session.tcp.TcpProxyData
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher

internal class DefaultProxyManagerFactory
@Inject
internal constructor(
    @ServerInternalApi private val dispatcher: CoroutineDispatcher,
    @ServerInternalApi private val tcpSession: ProxySession<TcpProxyData>,
    private val enforcer: ThreadEnforcer,
) : ProxyManager.Factory {

  @CheckResult
  private fun createTcp(): ProxyManager {
    return TcpProxyManager(
        enforcer = enforcer,
        dispatcher = dispatcher,
        session = tcpSession,
    )
  }

  override fun create(type: SharedProxy.Type): ProxyManager {
    return when (type) {
      SharedProxy.Type.TCP -> createTcp()
      SharedProxy.Type.UDP -> throw IllegalArgumentException("Unable to create UDP ProxyManager")
    }
  }
}
