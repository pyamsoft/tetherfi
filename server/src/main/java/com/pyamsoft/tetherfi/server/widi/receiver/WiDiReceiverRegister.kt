package com.pyamsoft.tetherfi.server.widi.receiver

interface WiDiReceiverRegister {

  suspend fun register()

  suspend fun unregister()
}
