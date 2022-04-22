package com.pyamsoft.widefi.server.widi.receiver

internal interface WiDiReceiver {

  suspend fun onEvent(onEvent: (Event) -> Unit)

  fun register()

  fun unregister()

  sealed class Event {
    object StateWifiEnabled : Event()
    object StateWifiDisabled : Event()
    object StatePeersChanged : Event()
    object StateConnectionChanged : Event()
    object StateDeviceChanged : Event()
  }
}
