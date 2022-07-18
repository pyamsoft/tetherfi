package com.pyamsoft.tetherfi.server.proxy.session.data

import com.pyamsoft.tetherfi.server.proxy.session.DestinationInfo
import com.pyamsoft.tetherfi.server.proxy.session.ProxyData
import com.pyamsoft.tetherfi.server.proxy.session.udp.tracker.KeyedObjectProducer
import io.ktor.network.sockets.BoundDatagramSocket
import io.ktor.network.sockets.ConnectedDatagramSocket
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
      val connectionProducer: KeyedObjectProducer<DestinationInfo, ConnectedDatagramSocket>,
  )
}
