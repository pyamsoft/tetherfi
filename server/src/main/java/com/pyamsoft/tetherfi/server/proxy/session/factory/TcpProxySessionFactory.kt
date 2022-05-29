package com.pyamsoft.tetherfi.server.proxy.session.factory

import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.tetherfi.server.ServerInternalApi
import com.pyamsoft.tetherfi.server.event.ConnectionEvent
import com.pyamsoft.tetherfi.server.event.ErrorEvent
import com.pyamsoft.tetherfi.server.proxy.session.ProxySession
import com.pyamsoft.tetherfi.server.proxy.session.TcpProxyOptions
import com.pyamsoft.tetherfi.server.proxy.session.TcpProxySession
import com.pyamsoft.tetherfi.server.proxy.session.UrlFixer
import com.pyamsoft.tetherfi.server.proxy.session.mempool.MemPool
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
) : ProxySession.Factory<TcpProxyOptions> {

  override fun create(): ProxySession<TcpProxyOptions> {
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
