package com.pyamsoft.tetherfi.server.clients

interface BlockedClientTracker {

  fun block(client: TetherClient)

  fun unblock(client: TetherClient)
}
