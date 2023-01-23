package com.pyamsoft.tetherfi.ui.qr

import android.graphics.Bitmap
import androidx.annotation.CheckResult
import com.pyamsoft.pydroid.arch.AbstractViewModeler
import io.github.g0dkar.qrcode.QRCode
import javax.inject.Inject
import javax.inject.Named
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class QRCodeViewModeler
@Inject
internal constructor(
    override val state: MutableQRCodeViewState,
    @Named("ssid") ssid: String,
    @Named("password") password: String,
) : AbstractViewModeler<QRCodeViewState>(state) {

  private val data =
      formatQRCode(
          ssid = ssid,
          password = password,
      )

  fun load(scope: CoroutineScope) {
    scope.launch(context = Dispatchers.IO) {
      val data = QRCode(data = data).render().nativeImage() as Bitmap
      state.qrCode.value = data
    }
  }

  companion object {

    @JvmStatic
    @CheckResult
    private fun formatQRCode(
        ssid: String,
        password: String,
    ): String {
      return "WIFI:T:WPA;S:${ssid};P:${password};H:;;"
    }
  }
}
