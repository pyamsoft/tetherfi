package com.pyamsoft.tetherfi.server.proxy.session.data

import com.pyamsoft.tetherfi.server.proxy.session.tcp.mempool.MemPool
import io.ktor.network.sockets.Socket

internal data class TcpProxyData
internal constructor(
    val proxy: Socket,
    val memPool: MemPool<ByteArray>,
) : ProxyData
