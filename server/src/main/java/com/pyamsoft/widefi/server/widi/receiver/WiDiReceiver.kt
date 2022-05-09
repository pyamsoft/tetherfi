package com.pyamsoft.widefi.server.widi.receiver

internal interface WiDiReceiver {

  suspend fun onEvent(onEvent: (WidiNetworkEvent) -> Unit)

  suspend fun register()

  suspend fun unregister()

}
