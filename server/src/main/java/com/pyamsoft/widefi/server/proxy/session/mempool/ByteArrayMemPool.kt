package com.pyamsoft.widefi.server.proxy.session.mempool

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class ByteArrayMemPool @Inject internal constructor() : UnboundedMemPool<ByteArray>() {

  override fun make(): ByteArray {
    return ByteArray(BUFFER_SIZE)
  }

  companion object {

    private const val BUFFER_SIZE = 8192
  }
}
