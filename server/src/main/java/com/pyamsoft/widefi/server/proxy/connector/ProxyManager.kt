package com.pyamsoft.widefi.server.proxy.connector

import androidx.annotation.CheckResult
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.core.Enforcer
import com.pyamsoft.widefi.server.ConnectionEvent
import com.pyamsoft.widefi.server.ErrorEvent
import com.pyamsoft.widefi.server.proxy.SharedProxy
import com.pyamsoft.widefi.server.proxy.tcp.TcpProxyConnection
import com.pyamsoft.widefi.server.proxy.udp.UdpProxyConnection
import com.pyamsoft.widefi.server.status.StatusBroadcast
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class ProxyManager
private constructor(
    private val proxyType: SharedProxy.Type,
    private val port: Int,
    private val dispatcher: CoroutineDispatcher,
    private val status: StatusBroadcast,
    private val errorBus: EventBus<ErrorEvent>,
    private val connectionBus: EventBus<ConnectionEvent>,
    private val proxyDebug: Boolean,
) {

  private fun CoroutineScope.loopTcp() {
    val tcp =
        TcpProxyConnection(
            port = port,
            status = status,
            dispatcher = dispatcher,
            errorBus = errorBus,
            connectionBus = connectionBus,
            proxyDebug = proxyDebug,
        )

    launch(context = dispatcher) { tcp.loop() }
  }

  private fun CoroutineScope.loopUdp() {
    val udp =
        UdpProxyConnection(
            port = port,
            status = status,
            dispatcher = dispatcher,
            errorBus = errorBus,
            connectionBus = connectionBus,
            proxyDebug = proxyDebug,
        )

    launch(context = dispatcher) { udp.loop() }
  }

  /** Loop this proxy connection, accepting new connections up to a connection limit */
  fun loop(scope: CoroutineScope) {
    Enforcer.assertOffMainThread()

    when (proxyType) {
      SharedProxy.Type.TCP -> scope.loopTcp()
      SharedProxy.Type.UDP -> scope.loopUdp()
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
        connectionBus: EventBus<ConnectionEvent>,
        proxyDebug: Boolean,
    ): ProxyManager {
      return ProxyManager(
          proxyType = SharedProxy.Type.TCP,
          port = port,
          status = status,
          dispatcher = dispatcher,
          errorBus = errorBus,
          connectionBus = connectionBus,
          proxyDebug = proxyDebug,
      )
    }

    @JvmStatic
    @CheckResult
    fun udp(
        port: Int,
        status: StatusBroadcast,
        dispatcher: CoroutineDispatcher,
        connectionBus: EventBus<ConnectionEvent>,
        errorBus: EventBus<ErrorEvent>,
        proxyDebug: Boolean,
    ): ProxyManager {
      return ProxyManager(
          proxyType = SharedProxy.Type.UDP,
          port = port,
          status = status,
          dispatcher = dispatcher,
          errorBus = errorBus,
          connectionBus = connectionBus,
          proxyDebug = proxyDebug,
      )
    }
  }
}
