/*
 * Copyright 2024 pyamsoft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pyamsoft.tetherfi.ui.qr

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.isUnspecified
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.ImageLoader
import coil.compose.AsyncImage
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.pydroid.ui.theme.ZeroSize
import com.pyamsoft.tetherfi.ui.test.createNewTestImageLoader

private enum class QRCodeScreenContentTypes {
  QR,
  MSG,
}

@Composable
fun QRCodeScreen(
    modifier: Modifier = Modifier,
    state: QRCodeViewState,
    imageLoader: ImageLoader,
) {
  val density = LocalDensity.current
  val contentDp = MaterialTheme.keylines.content
  val qrCode by state.qrCode.collectAsStateWithLifecycle()

  val (qrCodeSize, setQrCodeSize) = remember { mutableStateOf(Dp.Unspecified) }

  // QR Code needs a "quiet zone" in dark mode
  // https://github.com/pyamsoft/tetherfi/issues/251
  val (quietZoneSize, setQuietZoneSize) = remember { mutableStateOf(Dp.Unspecified) }

  LazyColumn(
      modifier = modifier,
  ) {
    item(
        contentType = QRCodeScreenContentTypes.QR,
    ) {
      Crossfade(
          label = "QRCode",
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
                    val w = size.width
                    val width = density.run { w.toDp() }

                    setQrCodeSize(width)

                    // QR Code needs a "quiet zone" in dark mode
                    // https://github.com/pyamsoft/tetherfi/issues/251
                    if (w > 0) {
                      val qz = (1.0F / w.toFloat() * 8).toInt()
                      val quietZoneDp = density.run { qz.toDp() }
                      if (quietZoneDp >= contentDp) {
                        setQuietZoneSize(quietZoneDp)
                      } else {
                        setQuietZoneSize(contentDp)
                      }
                    }
                  },
              contentAlignment = Alignment.Center,
          ) {
            // QR Code needs a "quiet zone" in dark mode
            // https://github.com/pyamsoft/tetherfi/issues/251

            // padding modifier can't handle unspecified
            val quietPadding =
                remember(quietZoneSize) {
                  if (quietZoneSize.isUnspecified) {
                    ZeroSize
                  } else {
                    quietZoneSize
                  }
                }
            Box(
                modifier =
                    Modifier.fillMaxWidth().background(color = Color.White).padding(quietPadding),
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
    }

    item(
        contentType = QRCodeScreenContentTypes.MSG,
    ) {
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
