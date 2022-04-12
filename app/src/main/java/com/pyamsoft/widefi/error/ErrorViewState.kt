package com.pyamsoft.widefi.error

import androidx.compose.runtime.Stable
import com.pyamsoft.pydroid.arch.UiViewState
import javax.inject.Inject

@Stable interface ErrorViewState : UiViewState

internal class MutableErrorViewState @Inject internal constructor() : ErrorViewState
