package com.pyamsoft.tetherfi.server.proxy.connector.factory

import androidx.annotation.CheckResult
import com.pyamsoft.tetherfi.server.ServerInternalApi
import com.pyamsoft.tetherfi.server.proxy.SharedProxy
import com.pyamsoft.tetherfi.server.proxy.connector.ProxyManager
import com.pyamsoft.tetherfi.server.proxy.connector.TcpProxyManager
import com.pyamsoft.tetherfi.server.proxy.connector.UdpProxyManager
import com.pyamsoft.tetherfi.server.proxy.session.ProxySession
import com.pyamsoft.tetherfi.server.proxy.session.data.TcpProxyData
import com.pyamsoft.tetherfi.server.proxy.session.data.UdpProxyData
import com.pyamsoft.tetherfi.server.proxy.session.tcp.mempool.ManagedMemPool
import com.pyamsoft.tetherfi.server.proxy.session.udp.connections.ManagedDatagramConnectionPool
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher

@Singleton
internal class DefaultProxyManagerFactory
@Inject
internal constructor(
    @ServerInternalApi private val proxyDebug: Boolean,
    @ServerInternalApi private val dispatcher: CoroutineDispatcher,
    @ServerInternalApi private val tcpSession: ProxySession<TcpProxyData>,
    @ServerInternalApi private val udpSession: ProxySession<UdpProxyData>,
    @ServerInternalApi private val memPoolProvider: Provider<ManagedMemPool<ByteArray>>,
    @ServerInternalApi private val connectionPoolProvider: Provider<ManagedDatagramConnectionPool>,
) : ProxyManager.Factory {

  @CheckResult
  private fun createTcp(port: Int): ProxyManager {
    return TcpProxyManager(
        port = port,
        dispatcher = dispatcher,
        proxyDebug = proxyDebug,
        session = tcpSession,
        memPoolProvider = memPoolProvider,
    )
  }

  @CheckResult
  private fun createUdp(port: Int): ProxyManager {
    return UdpProxyManager(
        port = port,
        dispatcher = dispatcher,
        proxyDebug = proxyDebug,
        session = udpSession,
        connectionPoolProvider = connectionPoolProvider,
    )
  }

  override fun create(type: SharedProxy.Type, port: Int): ProxyManager {
    return when (type) {
      SharedProxy.Type.TCP -> createTcp(port = port)
      SharedProxy.Type.UDP -> createUdp(port = port)
    }
  }
}
