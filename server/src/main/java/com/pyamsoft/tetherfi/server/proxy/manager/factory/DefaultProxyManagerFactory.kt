package com.pyamsoft.tetherfi.server.proxy.manager.factory

import androidx.annotation.CheckResult
import com.pyamsoft.tetherfi.server.ProxyDebug
import com.pyamsoft.tetherfi.server.ServerInternalApi
import com.pyamsoft.tetherfi.server.proxy.SharedProxy
import com.pyamsoft.tetherfi.server.proxy.manager.ProxyManager
import com.pyamsoft.tetherfi.server.proxy.manager.TcpProxyManager
import com.pyamsoft.tetherfi.server.proxy.session.ProxySession
import com.pyamsoft.tetherfi.server.proxy.session.tcp.TcpProxyData
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultProxyManagerFactory
@Inject
internal constructor(
    @ServerInternalApi private val tcpSession: ProxySession<TcpProxyData>,
    @ServerInternalApi private val proxyDebug: ProxyDebug,
) : ProxyManager.Factory {

  @CheckResult
  private fun createTcp(): ProxyManager {
    return TcpProxyManager(
        session = tcpSession,
        proxyDebug = proxyDebug,
    )
  }

  override fun create(type: SharedProxy.Type): ProxyManager {
    return when (type) {
      SharedProxy.Type.TCP -> createTcp()
      SharedProxy.Type.UDP -> throw IllegalArgumentException("Unable to create UDP ProxyManager")
    }
  }
}
