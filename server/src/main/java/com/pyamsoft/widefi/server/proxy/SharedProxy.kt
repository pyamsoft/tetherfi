package com.pyamsoft.widefi.server.proxy

import com.pyamsoft.widefi.server.Server

interface SharedProxy : Server {

  fun start()

  fun stop()

  enum class Type {
    TCP,
    UDP
  }
}
