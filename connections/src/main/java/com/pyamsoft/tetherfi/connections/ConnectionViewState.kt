package com.pyamsoft.tetherfi.connections

import androidx.compose.runtime.Stable
import com.pyamsoft.pydroid.arch.UiViewState
import com.pyamsoft.tetherfi.server.clients.TetherClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@Stable
interface ConnectionViewState : UiViewState {
  val connections: StateFlow<List<TetherClient>>
  val blocked: StateFlow<Set<TetherClient>>
}

@Stable
class MutableConnectionViewState @Inject internal constructor() : ConnectionViewState {
  override val connections = MutableStateFlow<List<TetherClient>>(emptyList())
  override val blocked = MutableStateFlow<Set<TetherClient>>(emptySet())
}
