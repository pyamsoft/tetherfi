package com.pyamsoft.tetherfi.server.widi.receiver

interface WiDiReceiver {

  suspend fun onEvent(onEvent: suspend (WidiNetworkEvent) -> Unit)
}
