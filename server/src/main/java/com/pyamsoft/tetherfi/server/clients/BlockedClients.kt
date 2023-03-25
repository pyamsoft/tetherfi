package com.pyamsoft.tetherfi.server.clients

import androidx.annotation.CheckResult
import kotlinx.coroutines.flow.Flow

interface BlockedClients {

  @CheckResult fun listenForBlocked(): Flow<Set<TetherClient>>

  @CheckResult suspend fun isBlocked(client: TetherClient): Boolean
}
