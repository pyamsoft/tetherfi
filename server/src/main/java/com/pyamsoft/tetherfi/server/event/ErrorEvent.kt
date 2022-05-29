package com.pyamsoft.tetherfi.server.event

import com.pyamsoft.tetherfi.core.LogEvent

sealed class ErrorEvent : LogEvent {

  data class Clear(
      override val id: String,
      override val clear: Boolean = true,
  ) : ErrorEvent()

  data class Tcp(
      override val id: String,
      val request: ProxyRequest?,
      val throwable: Throwable,
      override val clear: Boolean = false,
  ) : ErrorEvent()

  data class Udp(
      override val id: String,
      val throwable: Throwable,
      val hostName: String,
      val port: Int,
      override val clear: Boolean = false,
  ) : ErrorEvent()
}
