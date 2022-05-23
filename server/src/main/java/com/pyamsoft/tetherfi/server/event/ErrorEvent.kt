package com.pyamsoft.tetherfi.server.event

import com.pyamsoft.tetherfi.core.LogEvent

sealed class ErrorEvent : LogEvent {

  data class Clear(
      override val id: String,
  ) : ErrorEvent()

  data class Tcp(
      override val id: String,
      val request: ProxyRequest?,
      val throwable: Throwable,
  ) : ErrorEvent()

  data class Udp(
      override val id: String,
      val throwable: Throwable,
  ) : ErrorEvent()
}
