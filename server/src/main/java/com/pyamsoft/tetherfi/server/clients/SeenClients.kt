package com.pyamsoft.tetherfi.server.clients

import androidx.annotation.CheckResult
import kotlinx.coroutines.flow.Flow

interface SeenClients {

  @CheckResult fun listenForClients(): Flow<Set<TetherClient>>

  @CheckResult suspend fun seen(client: TetherClient)

  @CheckResult suspend fun clear()
}
