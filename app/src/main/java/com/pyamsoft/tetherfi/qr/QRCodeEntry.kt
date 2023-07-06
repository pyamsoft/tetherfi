/*
 * Copyright 2023 pyamsoft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.ImageLoader
import com.pyamsoft.pydroid.ui.app.rememberDialogProperties
import com.pyamsoft.pydroid.ui.defaults.DialogDefaults
import com.pyamsoft.pydroid.ui.haptics.rememberHapticManager
import com.pyamsoft.pydroid.ui.inject.ComposableInjector
import com.pyamsoft.pydroid.ui.inject.rememberComposableInjector
import com.pyamsoft.pydroid.ui.util.rememberNotNull
import com.pyamsoft.tetherfi.ObjectGraph
import com.pyamsoft.tetherfi.ui.DialogToolbar
import com.pyamsoft.tetherfi.ui.qr.QRCodeScreen
import com.pyamsoft.tetherfi.ui.qr.QRCodeViewModeler
import javax.inject.Inject

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

  val hapticManager = rememberHapticManager()

  LaunchedEffect(
      viewModel,
  ) {
    viewModel.load(scope = this)
  }

  // We need to tell this how large it is in advance or the Dialog sizes weird
  Dialog(
      properties = rememberDialogProperties(),
      onDismissRequest = onDismiss,
  ) {
    val qrCodeSize = rememberQRCodeWidth()

    Column(
        modifier = modifier.width(qrCodeSize),
    ) {
      DialogToolbar(
          modifier = Modifier.fillMaxWidth(),
          hapticManager = hapticManager,
          onClose = onDismiss,
          title = {
            Text(
                text = "QR Code",
            )
          },
      )

      Surface(
          modifier = Modifier.fillMaxWidth(),
          elevation = DialogDefaults.Elevation,
          shape =
              MaterialTheme.shapes.medium.copy(
                  topStart = ZeroCornerSize,
                  topEnd = ZeroCornerSize,
              ),
      ) {
        QRCodeScreen(
            modifier = Modifier.fillMaxWidth(),
            state = viewModel,
            imageLoader = imageLoader,
        )
      }
    }
  }
}
