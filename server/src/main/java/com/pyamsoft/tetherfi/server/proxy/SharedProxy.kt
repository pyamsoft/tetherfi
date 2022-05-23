package com.pyamsoft.tetherfi.server.proxy

import com.pyamsoft.tetherfi.server.Server

interface SharedProxy : Server {

  fun start()

  fun stop()

  enum class Type {
    TCP,
    UDP
  }
}
