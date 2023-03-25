package com.pyamsoft.tetherfi.server.clients

import java.time.LocalDateTime

sealed class TetherClient {
  data class IpAddress(
      val ip: String,
      val firstSeen: LocalDateTime,
  ) : TetherClient()
  data class HostName(
      val hostname: String,
      val firstSeen: LocalDateTime,
  ) : TetherClient()
}
