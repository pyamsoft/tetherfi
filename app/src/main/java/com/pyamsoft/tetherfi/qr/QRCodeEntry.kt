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

package com.pyamsoft.tetherfi.qr

import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil3.ImageLoader
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.pydroid.ui.app.rememberDialogProperties
import com.pyamsoft.pydroid.ui.inject.ComposableInjector
import com.pyamsoft.pydroid.ui.inject.rememberComposableInjector
import com.pyamsoft.pydroid.ui.util.rememberNotNull
import com.pyamsoft.tetherfi.ObjectGraph
import com.pyamsoft.tetherfi.ui.R
import com.pyamsoft.tetherfi.ui.dialog.DialogToolbar
import com.pyamsoft.tetherfi.ui.qr.QRCodeScreen
import com.pyamsoft.tetherfi.ui.qr.QRCodeViewModeler
import com.pyamsoft.tetherfi.ui.qr.QRCodeViewState
import com.pyamsoft.tetherfi.ui.test.createNewTestImageLoader
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import org.jetbrains.annotations.TestOnly

internal class QRCodeInjector(
    private val ssid: String,
    private val password: String,
) : ComposableInjector() {

  @JvmField @Inject internal var viewModel: QRCodeViewModeler? = null

  @JvmField @Inject internal var imageLoader: ImageLoader? = null

  override fun onInject(activity: ComponentActivity) {
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
private fun rememberQRCodeWidth(): Dp {
  val configuration = LocalConfiguration.current
  val orientation = configuration.orientation
  val width = configuration.screenWidthDp
  return remember(
      orientation,
      width,
  ) {
    if (orientation == Configuration.ORIENTATION_PORTRAIT) {
      width.dp / 4 * 3
    } else {
      width.dp / 3
    }
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

  QRCodeDialog(
      modifier = modifier,
      state = viewModel,
      imageLoader = imageLoader,
      onDismiss = onDismiss,
  )
}

@Composable
private fun QRCodeDialog(
    modifier: Modifier = Modifier,
    state: QRCodeViewState,
    imageLoader: ImageLoader,
    onDismiss: () -> Unit,
) {
  // We need to tell this how large it is in advance or the Dialog sizes weird
  Dialog(
      properties = rememberDialogProperties(),
      onDismissRequest = onDismiss,
  ) {
    val qrCodeSize = rememberQRCodeWidth()

    Column(
        modifier =
            modifier
                // Top already has padding for some reason?
                .padding(horizontal = MaterialTheme.keylines.content)
                .padding(bottom = MaterialTheme.keylines.content)
                .width(qrCodeSize),
    ) {
      DialogToolbar(
          modifier = Modifier.fillMaxWidth(),
          onClose = onDismiss,
          title = {
            Text(
                text = stringResource(R.string.qr_code),
            )
          },
      )

      Card(
          shape =
              MaterialTheme.shapes.large.copy(
                  topStart = ZeroCornerSize,
                  topEnd = ZeroCornerSize,
              ),
          elevation = CardDefaults.elevatedCardElevation(),
          colors = CardDefaults.elevatedCardColors(),
      ) {
        QRCodeScreen(
            modifier = Modifier.fillMaxWidth(),
            state = state,
            imageLoader = imageLoader,
        )
      }
    }
  }
}

@TestOnly
@Composable
private fun PreviewQRCodeDialog(bitmap: Bitmap?) {
  QRCodeDialog(
      state =
          object : QRCodeViewState {
            override val qrCode = MutableStateFlow(bitmap)
          },
      imageLoader = createNewTestImageLoader(),
      onDismiss = {},
  )
}

@Preview
@Composable
private fun PreviewQRCodeDialogNone() {
  PreviewQRCodeDialog(
      bitmap = null,
  )
}
