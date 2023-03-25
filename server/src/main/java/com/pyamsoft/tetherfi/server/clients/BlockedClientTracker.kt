package com.pyamsoft.tetherfi.server.clients

interface BlockedClientTracker {

  suspend fun block(client: TetherClient)

  suspend fun unblock(client: TetherClient)
}
