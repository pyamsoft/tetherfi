package com.pyamsoft.tetherfi.main

import androidx.compose.runtime.Stable
import com.pyamsoft.tetherfi.ui.ServerViewState
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Stable
interface MainViewState : ServerViewState {
  val isSettingsOpen: StateFlow<Boolean>
  val isShowingQRCodeDialog: StateFlow<Boolean>
}

@Stable
class MutableMainViewState @Inject internal constructor() : MainViewState {
  override val ssid = MutableStateFlow("")
  override val password = MutableStateFlow("")
  override val ip = MutableStateFlow("")
  override val port = MutableStateFlow(0)

  override val isSettingsOpen = MutableStateFlow(false)
  override val isShowingQRCodeDialog = MutableStateFlow(false)
}
