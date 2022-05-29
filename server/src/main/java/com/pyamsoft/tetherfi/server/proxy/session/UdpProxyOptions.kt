package com.pyamsoft.tetherfi.server.proxy.session

import io.ktor.network.sockets.Datagram

internal data class UdpProxyOptions
internal constructor(
    val sender: DatagramSender,
    val packet: Datagram,
)

fun interface DatagramSender {
  suspend fun send(packet: Datagram)
}
