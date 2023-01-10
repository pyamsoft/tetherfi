package com.pyamsoft.tetherfi.server.proxy.session.tcp

import com.pyamsoft.tetherfi.server.proxy.session.ProxyData
import com.pyamsoft.tetherfi.server.proxy.session.tcp.mempool.MemPool
import io.ktor.network.sockets.Socket

internal data class TcpProxyData
internal constructor(
    override val runtime: Runtime,
    override val environment: Environment,
) : ProxyData<TcpProxyData.Runtime, TcpProxyData.Environment> {

  internal data class Runtime(
      val connection: Socket,
  )

  internal data class Environment
  internal constructor(
      val memPool: MemPool<ByteArray>,
  )
}
