package com.pyamsoft.tetherfi.server.event

import com.pyamsoft.tetherfi.core.LogEvent

sealed class ConnectionEvent : LogEvent {

  data class Clear(
      override val id: String,
      override val clear: Boolean = true,
  ) : ConnectionEvent()

  data class Tcp(
      override val id: String,
      val request: ProxyRequest,
      override val clear: Boolean = false,
  ) : ConnectionEvent()

  data class Udp(
      override val id: String,
      val hostName: String,
      val port: Int,
      override val clear: Boolean = false,
  ) : ConnectionEvent()
}
