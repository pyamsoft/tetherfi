package com.pyamsoft.widefi.activity

import com.pyamsoft.pydroid.arch.AbstractViewModeler
import javax.inject.Inject

internal class ActivityViewModeler
@Inject
internal constructor(
    private val state: MutableActivityViewState,
) : AbstractViewModeler<ActivityViewState>(state) {}
