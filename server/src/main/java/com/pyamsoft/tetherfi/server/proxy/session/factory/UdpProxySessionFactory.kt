package com.pyamsoft.tetherfi.server.proxy.session.factory

import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.tetherfi.server.ServerInternalApi
import com.pyamsoft.tetherfi.server.event.ConnectionEvent
import com.pyamsoft.tetherfi.server.event.ErrorEvent
import com.pyamsoft.tetherfi.server.proxy.session.ProxySession
import com.pyamsoft.tetherfi.server.proxy.session.options.UdpProxyOptions
import com.pyamsoft.tetherfi.server.proxy.session.UdpProxySession
import com.pyamsoft.tetherfi.server.proxy.session.urlfixer.UrlFixer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class UdpProxySessionFactory
@Inject
internal constructor(
    // Need to use MutableSet instead of Set because of Java -> Kotlin fun.
    @ServerInternalApi private val urlFixers: MutableSet<UrlFixer>,
    @ServerInternalApi private val proxyDebug: Boolean,
    @ServerInternalApi private val errorBus: EventBus<ErrorEvent>,
    @ServerInternalApi private val connectionBus: EventBus<ConnectionEvent>,
) : ProxySession.Factory<UdpProxyOptions> {

  override fun create(): ProxySession<UdpProxyOptions> {
    return UdpProxySession(
        errorBus = errorBus,
        connectionBus = connectionBus,
        urlFixers = urlFixers,
        proxyDebug = proxyDebug,
    )
  }
}
