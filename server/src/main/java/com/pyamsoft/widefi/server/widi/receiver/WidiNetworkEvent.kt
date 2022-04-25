package com.pyamsoft.widefi.server.widi.receiver

sealed class WidiNetworkEvent {
  object WifiEnabled : WidiNetworkEvent()
  object WifiDisabled : WidiNetworkEvent()
  object PeersChanged : WidiNetworkEvent()
  object ConnectionChanged : WidiNetworkEvent()
  object DeviceChanged : WidiNetworkEvent()
}