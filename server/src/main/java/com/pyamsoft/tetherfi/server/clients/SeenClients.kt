package com.pyamsoft.tetherfi.server.clients

import androidx.annotation.CheckResult
import kotlinx.coroutines.flow.Flow

interface SeenClients {

  @CheckResult fun listenForClients(): Flow<Set<TetherClient>>

  @CheckResult fun seen(client: TetherClient)

  @CheckResult fun clear()
}
