package com.pyamsoft.tetherfi.server.event

import com.pyamsoft.tetherfi.core.LogEvent
import com.pyamsoft.tetherfi.server.proxy.session.DestinationInfo

sealed class ErrorEvent : LogEvent {

  data class Clear(
      override val id: String,
  ) : ErrorEvent() {
    override val clear: Boolean = true
  }

  data class Tcp(
      override val id: String,
      val request: ProxyRequest?,
      val throwable: Throwable,
  ) : ErrorEvent() {
    override val clear: Boolean = false
  }

  data class Udp(
      override val id: String,
      val throwable: Throwable,
      val destination: DestinationInfo,
  ) : ErrorEvent() {
    override val clear: Boolean = false
  }
}
