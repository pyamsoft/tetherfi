package com.pyamsoft.widefi.server.proxy.session.factory

import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.widefi.server.ConnectionEvent
import com.pyamsoft.widefi.server.ErrorEvent
import com.pyamsoft.widefi.server.proxy.session.ProxySession
import com.pyamsoft.widefi.server.proxy.session.TcpProxySession
import io.ktor.network.sockets.Socket
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher

@Singleton
internal class TcpProxySessionFactory
@Inject
internal constructor(
    @Named("proxy_debug") private val proxyDebug: Boolean,
    @Named("proxy_dispatcher") private val dispatcher: CoroutineDispatcher,
    private val errorBus: EventBus<ErrorEvent>,
    private val connectionBus: EventBus<ConnectionEvent>,
) : ProxySession.Factory<Socket> {

  override fun create(): ProxySession<Socket> {
    return TcpProxySession(
        dispatcher = dispatcher,
        errorBus = errorBus,
        connectionBus = connectionBus,
        proxyDebug = proxyDebug,
    )
  }
}
