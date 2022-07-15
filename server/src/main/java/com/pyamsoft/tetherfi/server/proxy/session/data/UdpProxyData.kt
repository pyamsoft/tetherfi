package com.pyamsoft.tetherfi.server.proxy.session.data

import io.ktor.network.sockets.BoundDatagramSocket
import io.ktor.network.sockets.Datagram

internal data class UdpProxyData
internal constructor(
    val proxy: BoundDatagramSocket,
    val initialPacket: Datagram,
) : ProxyData
