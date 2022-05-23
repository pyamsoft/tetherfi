package com.pyamsoft.tetherfi.server.widi.receiver

sealed class WidiNetworkEvent {
  object WifiEnabled : WidiNetworkEvent()
  object WifiDisabled : WidiNetworkEvent()
  object PeersChanged : WidiNetworkEvent()
  object ThisDeviceChanged : WidiNetworkEvent()
  object DiscoveryChanged : WidiNetworkEvent()
  data class ConnectionChanged internal constructor(val ip: String) : WidiNetworkEvent()
}
