package com.pyamsoft.tetherfi.server.widi.receiver

internal interface WiDiReceiver {

  suspend fun onEvent(onEvent: suspend (WidiNetworkEvent) -> Unit)

  suspend fun register()

  suspend fun unregister()
}
