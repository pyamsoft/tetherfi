package com.pyamsoft.tetherfi.tile

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.pyamsoft.pydroid.arch.UiViewState
import com.pyamsoft.tetherfi.server.status.RunningStatus
import javax.inject.Inject

@Stable
interface ProxyTileViewState : UiViewState {
  val isShowing: Boolean
  val status: RunningStatus
}

internal class MutableProxyTileViewState @Inject internal constructor() : ProxyTileViewState {
  override var isShowing by mutableStateOf(true)
  override var status by mutableStateOf<RunningStatus>(RunningStatus.NotRunning)
}
