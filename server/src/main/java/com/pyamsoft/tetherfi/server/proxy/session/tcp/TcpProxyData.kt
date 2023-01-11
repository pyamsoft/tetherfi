package com.pyamsoft.tetherfi.server.proxy.session.tcp

import com.pyamsoft.tetherfi.server.proxy.session.ProxyData
import io.ktor.network.sockets.Socket

internal data class TcpProxyData
internal constructor(
    internal val connection: Socket,
) : ProxyData
