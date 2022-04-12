package com.pyamsoft.widefi.error

import com.pyamsoft.pydroid.arch.AbstractViewModeler
import javax.inject.Inject

internal class ErrorViewModeler
@Inject
internal constructor(
    private val state: MutableErrorViewState,
) : AbstractViewModeler<ErrorViewState>(state) {}
