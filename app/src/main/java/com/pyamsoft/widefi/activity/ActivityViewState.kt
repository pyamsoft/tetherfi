package com.pyamsoft.widefi.activity

import androidx.compose.runtime.Stable
import com.pyamsoft.pydroid.arch.UiViewState
import javax.inject.Inject

@Stable interface ActivityViewState : UiViewState

internal class MutableActivityViewState @Inject internal constructor() : ActivityViewState
