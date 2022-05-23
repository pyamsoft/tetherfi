package com.pyamsoft.tetherfi.server.event

import com.pyamsoft.tetherfi.core.LogEvent

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
