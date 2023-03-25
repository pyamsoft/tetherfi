package com.pyamsoft.tetherfi.server.clients

import androidx.annotation.CheckResult
import java.time.LocalDateTime

sealed class TetherClient(
    open val firstSeen: LocalDateTime,
) {
  data class IpAddress(
      val ip: String,
      override val firstSeen: LocalDateTime,
  ) :
      TetherClient(
          firstSeen = firstSeen,
      )
  data class HostName(
      val hostname: String,
      override val firstSeen: LocalDateTime,
  ) :
      TetherClient(
          firstSeen = firstSeen,
      )
}

@CheckResult
fun TetherClient.key(): String {
    return when (this) {
        is TetherClient.HostName -> this.hostname
        is TetherClient.IpAddress -> this.ip
    }
}
