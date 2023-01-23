package com.pyamsoft.tetherfi.ui.qr

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import coil.ImageLoader
import coil.compose.AsyncImage
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.tetherfi.ui.test.createNewTestImageLoader

@Composable
fun QRCodeScreen(
    modifier: Modifier = Modifier,
    state: QRCodeViewState,
    imageLoader: ImageLoader,
) {
  val qrCode by state.qrCode.collectAsState()
  val (qrCodeSize, setQrCodeSize) = remember { mutableStateOf(Dp.Unspecified) }
  val density = LocalDensity.current

  LazyColumn(
      modifier = modifier,
  ) {
    item {
      Crossfade(
          targetState = qrCode,
      ) { code ->
        if (code == null) {
          Box(
              modifier = Modifier.fillMaxWidth().padding(MaterialTheme.keylines.content),
              contentAlignment = Alignment.Center,
          ) {
            CircularProgressIndicator()
          }
        } else {
          Box(
              modifier =
                  Modifier.fillMaxWidth().padding(MaterialTheme.keylines.content).onSizeChanged {
                      size ->
                    val width = density.run { size.width.toDp() }
                    setQrCodeSize(width)
                  },
              contentAlignment = Alignment.Center,
          ) {
            AsyncImage(
                modifier = Modifier.size(qrCodeSize),
                model = code,
                imageLoader = imageLoader,
                contentDescription = "QR Code",
                contentScale = ContentScale.FillBounds,
            )
          }
        }
      }
    }

    item {
      Text(
          modifier = Modifier.fillMaxWidth().padding(MaterialTheme.keylines.content),
          text = "Scan this QR code to connect",
      )
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
