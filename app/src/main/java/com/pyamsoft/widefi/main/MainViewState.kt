package com.pyamsoft.widefi.main

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.pyamsoft.pydroid.arch.UiViewState
import com.pyamsoft.widefi.server.status.RunningStatus
import com.pyamsoft.widefi.server.widi.WiDiNetwork
import javax.inject.Inject

@Stable
interface MainViewState : UiViewState {
  val group: WiDiNetwork.GroupInfo?
  val wiDiStatus: RunningStatus
  val proxyStatus: RunningStatus
  val view: MainView
}

internal class MutableMainViewState @Inject internal constructor() : MainViewState {
  override var group by mutableStateOf<WiDiNetwork.GroupInfo?>(null)
  override var wiDiStatus by mutableStateOf<RunningStatus>(RunningStatus.NotRunning)
  override var proxyStatus by mutableStateOf<RunningStatus>(RunningStatus.NotRunning)
  override var view by mutableStateOf(MainView.STATUS)
}
