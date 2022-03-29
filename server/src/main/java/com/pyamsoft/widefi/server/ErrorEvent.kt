package com.pyamsoft.widefi.server

sealed class ErrorEvent {

  data class Tcp(
      val request: ProxyRequest?,
      val throwable: Throwable,
  ) : ErrorEvent()

  class Udp(
      val throwable: Throwable,
  ) : ErrorEvent()
}
