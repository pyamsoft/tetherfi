package com.pyamsoft.tetherfi.server.broadcast

import androidx.annotation.CheckResult
import kotlinx.coroutines.flow.Flow

interface BroadcastObserver {

  @CheckResult fun listenNetworkEvents(): Flow<BroadcastEvent>
}
