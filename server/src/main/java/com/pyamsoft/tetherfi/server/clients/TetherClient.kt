package com.pyamsoft.tetherfi.server.clients

sealed class TetherClient {
  data class IpAddress(val ip: String) : TetherClient()
  data class HostName(val hostname: String) : TetherClient()
}
