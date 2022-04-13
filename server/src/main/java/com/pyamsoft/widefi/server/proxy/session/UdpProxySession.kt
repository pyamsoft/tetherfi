package com.pyamsoft.widefi.server.proxy.session

import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.core.Enforcer
import com.pyamsoft.widefi.server.event.ConnectionEvent
import com.pyamsoft.widefi.server.event.ErrorEvent
import com.pyamsoft.widefi.server.proxy.SharedProxy
import io.ktor.network.sockets.Datagram
import kotlinx.coroutines.CoroutineDispatcher
import timber.log.Timber

internal class UdpProxySession
internal constructor(
    private val dispatcher: CoroutineDispatcher,
    private val errorBus: EventBus<ErrorEvent>,
    private val connectionBus: EventBus<ConnectionEvent>,
    proxyDebug: Boolean,
) : BaseProxySession<Datagram>(SharedProxy.Type.UDP, proxyDebug) {

  override suspend fun exchange(proxy: Datagram) {
    Enforcer.assertOffMainThread()

    Timber.d("Attempt data exchange of UDP proxy: ${proxy.address}")
  }
}
