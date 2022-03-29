package com.pyamsoft.widefi.server

sealed class ConnectionEvent {

  object Clear : ConnectionEvent()

  data class Tcp(
      val request: ProxyRequest,
  ) : ConnectionEvent()

  object Udp : ConnectionEvent()
}
