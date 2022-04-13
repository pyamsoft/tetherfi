package com.pyamsoft.widefi.server.proxy.session.factory

import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.widefi.server.ConnectionEvent
import com.pyamsoft.widefi.server.ErrorEvent
import com.pyamsoft.widefi.server.ServerInternalApi
import com.pyamsoft.widefi.server.proxy.session.ProxySession
import com.pyamsoft.widefi.server.proxy.session.TcpProxySession
import com.pyamsoft.widefi.server.proxy.session.UrlFixer
import com.pyamsoft.widefi.server.proxy.session.mempool.MemPool
import io.ktor.network.sockets.Socket
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher

@Singleton
internal class TcpProxySessionFactory
@Inject
internal constructor(
    // Need to use MutableSet instead of Set because of Java -> Kotlin fun.
    @ServerInternalApi private val urlFixers: MutableSet<UrlFixer>,
    @ServerInternalApi private val proxyDebug: Boolean,
    @ServerInternalApi private val dispatcher: CoroutineDispatcher,
    @ServerInternalApi private val errorBus: EventBus<ErrorEvent>,
    @ServerInternalApi private val connectionBus: EventBus<ConnectionEvent>,
    @ServerInternalApi private val memPool: MemPool<ByteArray>
) : ProxySession.Factory<Socket> {

  override fun create(): ProxySession<Socket> {
    return TcpProxySession(
        dispatcher = dispatcher,
        errorBus = errorBus,
        connectionBus = connectionBus,
        proxyDebug = proxyDebug,
        urlFixers = urlFixers,
        memPool = memPool,
    )
  }
}
