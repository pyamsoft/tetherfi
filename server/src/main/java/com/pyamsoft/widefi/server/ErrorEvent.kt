package com.pyamsoft.widefi.server

sealed class ErrorEvent {

  object Clear : ErrorEvent()

  data class Tcp(
      val request: ProxyRequest?,
      val throwable: Throwable,
  ) : ErrorEvent()

  data class Udp(
      val throwable: Throwable,
  ) : ErrorEvent()
}
