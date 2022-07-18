package com.pyamsoft.tetherfi.server.event

import com.pyamsoft.tetherfi.core.LogEvent
import com.pyamsoft.tetherfi.server.proxy.session.DestinationInfo

sealed class ConnectionEvent : LogEvent {

  data class Clear(
      override val id: String,
  ) : ConnectionEvent() {
    override val clear: Boolean = true
  }

  data class Tcp(
      override val id: String,
      val request: ProxyRequest,
  ) : ConnectionEvent() {
    override val clear: Boolean = false
  }

  data class Udp(
      override val id: String,
      val destination: DestinationInfo,
  ) : ConnectionEvent() {
    override val clear: Boolean = false
  }
}
