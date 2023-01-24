package com.pyamsoft.tetherfi.info

import com.pyamsoft.pydroid.arch.AbstractViewModeler
import javax.inject.Inject
import kotlinx.coroutines.flow.update

class InfoViewModeler
@Inject
internal constructor(
    override val state: MutableInfoViewState,
) : AbstractViewModeler<InfoViewState>(state) {

  fun handleTogglePasswordVisibility() {
    state.isPasswordVisible.update { !it }
  }
}
