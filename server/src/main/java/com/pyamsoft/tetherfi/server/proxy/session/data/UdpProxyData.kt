package com.pyamsoft.tetherfi.server.proxy.session.data

import com.pyamsoft.tetherfi.server.proxy.session.udp.tracker.ConnectionTracker
import io.ktor.network.sockets.BoundDatagramSocket
import io.ktor.network.sockets.Datagram

internal data class UdpProxyData
internal constructor(
    override val runtime: Runtime,
    override val environment: Environment,
) : ProxyData<UdpProxyData.Runtime, UdpProxyData.Environment> {

  internal data class Runtime
  internal constructor(
      val proxy: BoundDatagramSocket,
      val initialPacket: Datagram,
  )

  internal data class Environment
  internal constructor(
      val tracker: ConnectionTracker,
  )
}
