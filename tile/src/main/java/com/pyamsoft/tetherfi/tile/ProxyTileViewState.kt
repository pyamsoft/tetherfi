package com.pyamsoft.tetherfi.tile

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.pyamsoft.pydroid.arch.UiViewState
import com.pyamsoft.tetherfi.server.status.RunningStatus
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Stable
interface ProxyTileViewState : UiViewState {
  val isShowing: StateFlow<Boolean>
  val status: StateFlow<RunningStatus>
}

@Stable
class MutableProxyTileViewState @Inject internal constructor() : ProxyTileViewState {
  override val isShowing = MutableStateFlow(false)
  override val status = MutableStateFlow<RunningStatus>(RunningStatus.NotRunning)
}
