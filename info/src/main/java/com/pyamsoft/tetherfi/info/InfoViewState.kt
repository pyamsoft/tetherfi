package com.pyamsoft.tetherfi.info

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.pyamsoft.pydroid.arch.UiViewState
import com.pyamsoft.tetherfi.core.ActivityScope
import javax.inject.Inject

interface InfoViewState : UiViewState {
  val ssid: String
  val password: String
  val ip: String
  val port: Int
}

@ActivityScope
internal class MutableInfoViewState @Inject internal constructor() : InfoViewState {
  override var ssid by mutableStateOf("")
  override var password by mutableStateOf("")
  override var ip by mutableStateOf("")
  override var port by mutableStateOf(0)
}
