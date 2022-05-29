package com.pyamsoft.tetherfi.server.proxy.session

import io.ktor.network.sockets.Socket

internal data class TcpProxyOptions internal constructor(
    val proxyConnection: Socket,
)
