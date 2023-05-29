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

package com.pyamsoft.tetherfi.server.proxy.manager

import androidx.annotation.CheckResult
import com.pyamsoft.pydroid.core.ThreadEnforcer
import com.pyamsoft.tetherfi.server.ProxyDebug
import com.pyamsoft.tetherfi.server.proxy.ProxyLogger
import com.pyamsoft.tetherfi.server.proxy.SharedProxy
import com.pyamsoft.tetherfi.server.proxy.session.tagSocket
import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.ASocket
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.SocketAddress
import io.ktor.network.sockets.SocketBuilder
import io.ktor.network.sockets.aSocket
import kotlin.coroutines.coroutineContext

internal abstract class BaseProxyManager<S : ASocket>(
    proxyType: SharedProxy.Type,
    proxyDebug: ProxyDebug,
    private val enforcer: ThreadEnforcer,
) :
    ProxyManager,
    ProxyLogger(
        proxyType,
        proxyDebug,
    ) {

  @CheckResult
  private fun getServerAddress(port: Int): SocketAddress {
    return InetSocketAddress(hostname = "0.0.0.0", port = port)
  }

  override suspend fun loop(port: Int) {
    enforcer.assertOffMainThread()

    // Tag sockets for Android O strict mode
    tagSocket()

    val server =
        openServer(
            builder = aSocket(ActorSelectorManager(context = coroutineContext)),
            localAddress = getServerAddress(port = port),
        )
    try {
      runServer(server)
    } finally {
      server.dispose()
      onServerClosed()
    }
  }

  protected abstract suspend fun runServer(server: S)

  @CheckResult
  protected abstract suspend fun openServer(
      builder: SocketBuilder,
      localAddress: SocketAddress,
  ): S

  @CheckResult protected abstract suspend fun onServerClosed()
}
