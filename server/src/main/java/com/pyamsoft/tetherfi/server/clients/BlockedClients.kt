package com.pyamsoft.tetherfi.server.clients

import androidx.annotation.CheckResult
import kotlinx.coroutines.flow.Flow

interface BlockedClients {

  @CheckResult fun listenForBlocked(): Flow<Set<TetherClient>>

  @CheckResult fun isBlocked(client: TetherClient): Boolean
}
