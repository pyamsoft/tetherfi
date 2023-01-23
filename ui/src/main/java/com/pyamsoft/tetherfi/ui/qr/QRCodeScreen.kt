package com.pyamsoft.tetherfi.ui.qr

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import coil.ImageLoader
import coil.compose.AsyncImage
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.pydroid.ui.defaults.DialogDefaults
import com.pyamsoft.tetherfi.ui.test.createNewTestImageLoader

@Composable
fun QRCodeScreen(
    modifier: Modifier = Modifier,
    state: QRCodeViewState,
    imageLoader: ImageLoader,
) {
  val qrCode by state.qrCode.collectAsState()

  Surface(
      modifier = modifier.padding(MaterialTheme.keylines.content),
      elevation = DialogDefaults.Elevation,
      shape = MaterialTheme.shapes.medium,
  ) {
    Crossfade(
        targetState = qrCode,
    ) { code ->
      if (code == null) {
        Box(
            contentAlignment = Alignment.Center,
        ) {
          CircularProgressIndicator()
        }
      } else {
        AsyncImage(
            model = code,
            imageLoader = imageLoader,
            contentDescription = "QR Code",
        )
      }
    }
  }
}

@Preview
@Composable
private fun PreviewQRCodeScreen() {
  QRCodeScreen(
      state = MutableQRCodeViewState(),
      imageLoader = createNewTestImageLoader(),
  )
}
