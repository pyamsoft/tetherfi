package com.pyamsoft.widefi.server.proxy.session.factory

import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.widefi.server.ServerInternalApi
import com.pyamsoft.widefi.server.event.ConnectionEvent
import com.pyamsoft.widefi.server.event.ErrorEvent
import com.pyamsoft.widefi.server.proxy.session.ProxySession
import com.pyamsoft.widefi.server.proxy.session.UdpProxySession
import io.ktor.network.sockets.Datagram
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher

@Singleton
internal class UdpProxySessionFactory
@Inject
internal constructor(
    @ServerInternalApi private val proxyDebug: Boolean,
    @ServerInternalApi private val dispatcher: CoroutineDispatcher,
    @ServerInternalApi private val errorBus: EventBus<ErrorEvent>,
    @ServerInternalApi private val connectionBus: EventBus<ConnectionEvent>,
) : ProxySession.Factory<Datagram> {

  override fun create(): ProxySession<Datagram> {
    return UdpProxySession(
        dispatcher = dispatcher,
        errorBus = errorBus,
        connectionBus = connectionBus,
        proxyDebug = proxyDebug,
    )
  }
}
