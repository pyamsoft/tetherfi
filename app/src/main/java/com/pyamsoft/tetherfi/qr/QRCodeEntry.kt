package com.pyamsoft.tetherfi.qr

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.fragment.app.FragmentActivity
import coil.ImageLoader
import com.pyamsoft.pydroid.ui.inject.ComposableInjector
import com.pyamsoft.pydroid.ui.inject.rememberComposableInjector
import com.pyamsoft.pydroid.ui.util.rememberNotNull
import com.pyamsoft.tetherfi.ObjectGraph
import com.pyamsoft.tetherfi.ui.qr.QRCodeScreen
import com.pyamsoft.tetherfi.ui.qr.QRCodeViewModeler
import javax.inject.Inject

internal class QRCodeInjector(
    private val ssid: String,
    private val password: String,
) : ComposableInjector() {

  @JvmField @Inject internal var viewModel: QRCodeViewModeler? = null
  @JvmField @Inject internal var imageLoader: ImageLoader? = null

  override fun onInject(activity: FragmentActivity) {
    ObjectGraph.ActivityScope.retrieve(activity)
        .plusQR()
        .create(
            ssid = ssid,
            password = password,
        )
        .inject(this)
  }

  override fun onDispose() {
    viewModel = null
    imageLoader = null
  }
}

@Composable
fun QRCodeEntry(
    modifier: Modifier = Modifier,
    ssid: String,
    password: String,
    onDismiss: () -> Unit,
) {
  val component = rememberComposableInjector {
    QRCodeInjector(
        ssid = ssid,
        password = password,
    )
  }
  val viewModel = rememberNotNull(component.viewModel)
  val imageLoader = rememberNotNull(component.imageLoader)

  LaunchedEffect(
      viewModel,
  ) {
    viewModel.load(scope = this)
  }

  Dialog(
      onDismissRequest = onDismiss,
  ) {
    QRCodeScreen(
        modifier = modifier,
        state = viewModel.state,
        imageLoader = imageLoader,
    )
  }
}
