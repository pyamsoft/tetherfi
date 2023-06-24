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

package com.pyamsoft.tetherfi.ui.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

@Suppress("UnusedReceiverParameter")
val Icons.Filled.QrCode: ImageVector
  get() {
    if (_qrCode != null) {
      return _qrCode!!
    }
    _qrCode =
        materialIcon(name = "Filled.QrCode") {
          materialPath {
            moveTo(3.0f, 11.0f)
            horizontalLineToRelative(8.0f)
            verticalLineTo(3.0f)
            horizontalLineTo(3.0f)
            verticalLineTo(11.0f)
            close()
            moveTo(5.0f, 5.0f)
            horizontalLineToRelative(4.0f)
            verticalLineToRelative(4.0f)
            horizontalLineTo(5.0f)
            verticalLineTo(5.0f)
            close()
          }
          materialPath {
            moveTo(3.0f, 21.0f)
            horizontalLineToRelative(8.0f)
            verticalLineToRelative(-8.0f)
            horizontalLineTo(3.0f)
            verticalLineTo(21.0f)
            close()
            moveTo(5.0f, 15.0f)
            horizontalLineToRelative(4.0f)
            verticalLineToRelative(4.0f)
            horizontalLineTo(5.0f)
            verticalLineTo(15.0f)
            close()
          }
          materialPath {
            moveTo(13.0f, 3.0f)
            verticalLineToRelative(8.0f)
            horizontalLineToRelative(8.0f)
            verticalLineTo(3.0f)
            horizontalLineTo(13.0f)
            close()
            moveTo(19.0f, 9.0f)
            horizontalLineToRelative(-4.0f)
            verticalLineTo(5.0f)
            horizontalLineToRelative(4.0f)
            verticalLineTo(9.0f)
            close()
          }
          materialPath {
            moveTo(19.0f, 19.0f)
            horizontalLineToRelative(2.0f)
            verticalLineToRelative(2.0f)
            horizontalLineToRelative(-2.0f)
            close()
          }
          materialPath {
            moveTo(13.0f, 13.0f)
            horizontalLineToRelative(2.0f)
            verticalLineToRelative(2.0f)
            horizontalLineToRelative(-2.0f)
            close()
          }
          materialPath {
            moveTo(15.0f, 15.0f)
            horizontalLineToRelative(2.0f)
            verticalLineToRelative(2.0f)
            horizontalLineToRelative(-2.0f)
            close()
          }
          materialPath {
            moveTo(13.0f, 17.0f)
            horizontalLineToRelative(2.0f)
            verticalLineToRelative(2.0f)
            horizontalLineToRelative(-2.0f)
            close()
          }
          materialPath {
            moveTo(15.0f, 19.0f)
            horizontalLineToRelative(2.0f)
            verticalLineToRelative(2.0f)
            horizontalLineToRelative(-2.0f)
            close()
          }
          materialPath {
            moveTo(17.0f, 17.0f)
            horizontalLineToRelative(2.0f)
            verticalLineToRelative(2.0f)
            horizontalLineToRelative(-2.0f)
            close()
          }
          materialPath {
            moveTo(17.0f, 13.0f)
            horizontalLineToRelative(2.0f)
            verticalLineToRelative(2.0f)
            horizontalLineToRelative(-2.0f)
            close()
          }
          materialPath {
            moveTo(19.0f, 15.0f)
            horizontalLineToRelative(2.0f)
            verticalLineToRelative(2.0f)
            horizontalLineToRelative(-2.0f)
            close()
          }
        }
    return _qrCode!!
  }

@Suppress("ObjectPropertyName") private var _qrCode: ImageVector? = null
