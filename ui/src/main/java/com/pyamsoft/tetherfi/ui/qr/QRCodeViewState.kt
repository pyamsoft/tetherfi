package com.pyamsoft.tetherfi.ui.qr

import android.graphics.Bitmap
import androidx.compose.runtime.Stable
import com.pyamsoft.pydroid.arch.UiViewState
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Stable
interface QRCodeViewState : UiViewState {
  val qrCode: StateFlow<Bitmap?>
}

@Stable
class MutableQRCodeViewState @Inject internal constructor() : QRCodeViewState {
  override val qrCode = MutableStateFlow<Bitmap?>(null)
}
