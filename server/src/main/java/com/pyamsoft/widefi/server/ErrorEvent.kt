package com.pyamsoft.widefi.server

import com.pyamsoft.widefi.server.proxy.SharedProxy

sealed class ErrorEvent {

  data class Tcp(
      val request: ProxyRequest?,
      val type: SharedProxy.Type,
      val throwable: Throwable,
  ) : ErrorEvent()

  class Udp(
      val type: SharedProxy.Type,
      val throwable: Throwable,
  ) : ErrorEvent()
}