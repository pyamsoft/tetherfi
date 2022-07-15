package com.pyamsoft.tetherfi.server.proxy.session.tcp.mempool

import javax.inject.Inject

// Not singleton since this will be provided each time by a Provider<>
internal class ByteArrayMemPool @Inject internal constructor() : UnboundedMemPool<ByteArray>() {

  override fun make(): ByteArray {
    return ByteArray(BUFFER_SIZE)
  }

  companion object {

    private const val BUFFER_SIZE = 8192
  }
}
