package com.pyamsoft.tetherfi.server.event

import com.pyamsoft.tetherfi.core.LogEvent
import com.pyamsoft.tetherfi.server.proxy.session.DestinationInfo

sealed class ErrorEvent : LogEvent {

  data class Clear(
      override val id: String,
      override val clear: Boolean = true,
  ) : ErrorEvent()

  data class Tcp(
      override val id: String,
      override val clear: Boolean = false,
      val request: ProxyRequest?,
      val throwable: Throwable,
  ) : ErrorEvent()

  data class Udp(
      override val id: String,
      override val clear: Boolean = false,
      val throwable: Throwable,
      val destination: DestinationInfo,
  ) : ErrorEvent()
}
