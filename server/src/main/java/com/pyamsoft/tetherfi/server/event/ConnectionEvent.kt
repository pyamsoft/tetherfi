package com.pyamsoft.tetherfi.server.event

import com.pyamsoft.tetherfi.core.LogEvent
import com.pyamsoft.tetherfi.server.proxy.session.DestinationInfo

sealed class ConnectionEvent : LogEvent {

  data class Clear(
      override val id: String,
      override val clear: Boolean = true,
  ) : ConnectionEvent()

  data class Tcp(
      override val id: String,
      override val clear: Boolean = false,
      val request: ProxyRequest,
  ) : ConnectionEvent()

  data class Udp(
      override val id: String,
      override val clear: Boolean = false,
      val destination: DestinationInfo,
  ) : ConnectionEvent()
}
