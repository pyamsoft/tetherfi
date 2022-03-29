package com.pyamsoft.widefi.server.proxy.connector

import androidx.annotation.CheckResult
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.core.Enforcer
import com.pyamsoft.widefi.server.ErrorEvent
import com.pyamsoft.widefi.server.proxy.SharedProxy
import com.pyamsoft.widefi.server.proxy.tcp.TcpProxyConnection
import com.pyamsoft.widefi.server.proxy.udp.UdpProxyConnection
import com.pyamsoft.widefi.server.status.StatusBroadcast
import kotlinx.coroutines.CoroutineDispatcher

internal class ProxyManager
private constructor(
    private val proxyType: SharedProxy.Type,
    private val port: Int,
    private val dispatcher: CoroutineDispatcher,
    private val status: StatusBroadcast,
    private val errorBus: EventBus<ErrorEvent>,
    private val proxyDebug: Boolean,
) {

  private suspend fun loopTcp() {
    TcpProxyConnection(
            port = port,
            status = status,
            dispatcher = dispatcher,
            errorBus = errorBus,
            proxyDebug = proxyDebug,
        )
        .loop()
  }

  private suspend fun loopUdp() {
    UdpProxyConnection(
            port = port,
            status = status,
            dispatcher = dispatcher,
            errorBus = errorBus,
            proxyDebug = proxyDebug,
        )
        .loop()
  }

  /** Loop this proxy connection, accepting new connections up to a connection limit */
  suspend fun loop() {
    Enforcer.assertOffMainThread()

    when (proxyType) {
      SharedProxy.Type.TCP -> loopTcp()
      SharedProxy.Type.UDP -> loopUdp()
    }
  }

  companion object {

    @JvmStatic
    @CheckResult
    fun tcp(
        port: Int,
        status: StatusBroadcast,
        dispatcher: CoroutineDispatcher,
        errorBus: EventBus<ErrorEvent>,
        proxyDebug: Boolean,
    ): ProxyManager {
      return ProxyManager(
          proxyType = SharedProxy.Type.TCP,
          port = port,
          status = status,
          dispatcher = dispatcher,
          errorBus = errorBus,
          proxyDebug = proxyDebug,
      )
    }

    @JvmStatic
    @CheckResult
    fun udp(
        port: Int,
        status: StatusBroadcast,
        dispatcher: CoroutineDispatcher,
        errorBus: EventBus<ErrorEvent>,
        proxyDebug: Boolean,
    ): ProxyManager {
      return ProxyManager(
          proxyType = SharedProxy.Type.UDP,
          port = port,
          status = status,
          dispatcher = dispatcher,
          errorBus = errorBus,
          proxyDebug = proxyDebug,
      )
    }
  }
}
