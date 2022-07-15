package com.pyamsoft.tetherfi.server.proxy.session.udp.connections

import io.ktor.network.sockets.Datagram

interface DatagramConnectionPool {

  suspend fun use(datagram: Datagram, block: suspend (datagram: Datagram) -> Unit)
}
