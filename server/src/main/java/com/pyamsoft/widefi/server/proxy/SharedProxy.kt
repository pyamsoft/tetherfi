package com.pyamsoft.widefi.server.proxy

import com.pyamsoft.widefi.server.Server

interface SharedProxy : Server {

  enum class Type {
    TCP,
    UDP
  }
}
