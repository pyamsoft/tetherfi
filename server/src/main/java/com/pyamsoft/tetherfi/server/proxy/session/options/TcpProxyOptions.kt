package com.pyamsoft.tetherfi.server.proxy.session.options

import io.ktor.network.sockets.Socket

internal data class TcpProxyOptions
internal constructor(
    val proxy: Socket,
) : ProxyOptions
