package com.pyamsoft.widefi.server.event

import com.pyamsoft.widefi.core.LogEvent

sealed class ConnectionEvent : LogEvent {

  data class Clear(
      override val id: String,
  ) : ConnectionEvent()

  data class Tcp(
      override val id: String,
      val request: ProxyRequest,
  ) : ConnectionEvent()

  data class Udp(
      override val id: String,
  ) : ConnectionEvent()
}
