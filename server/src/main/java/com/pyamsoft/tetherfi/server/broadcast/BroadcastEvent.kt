package com.pyamsoft.tetherfi.server.broadcast

sealed interface BroadcastEvent {

  /** Connection info has been updated by the broadcast */
  data class ConnectionChanged
  internal constructor(
      val hostName: String,
  ) : BroadcastEvent

  /** Other currently unhandled event */
  data object Other : BroadcastEvent
}
