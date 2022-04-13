package com.pyamsoft.widefi.server.event

sealed class ConnectionEvent {

  object Clear : ConnectionEvent()

  data class Tcp(
      val request: ProxyRequest,
  ) : ConnectionEvent()

  object Udp : ConnectionEvent()
}
