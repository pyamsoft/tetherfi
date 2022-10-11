package com.pyamsoft.tetherfi.ui.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

@Suppress("unused")
val Icons.Filled.PhoneAndroid: ImageVector
  get() {
    if (_phoneAndroid != null) {
      return _phoneAndroid!!
    }
    _phoneAndroid =
        materialIcon(name = "Filled.PhoneAndroid") {
          materialPath {
            moveTo(16.0f, 1.0f)
            lineTo(8.0f, 1.0f)
            curveTo(6.34f, 1.0f, 5.0f, 2.34f, 5.0f, 4.0f)
            verticalLineToRelative(16.0f)
            curveToRelative(0.0f, 1.66f, 1.34f, 3.0f, 3.0f, 3.0f)
            horizontalLineToRelative(8.0f)
            curveToRelative(1.66f, 0.0f, 3.0f, -1.34f, 3.0f, -3.0f)
            lineTo(19.0f, 4.0f)
            curveToRelative(0.0f, -1.66f, -1.34f, -3.0f, -3.0f, -3.0f)
            close()
            moveTo(14.0f, 21.0f)
            horizontalLineToRelative(-4.0f)
            verticalLineToRelative(-1.0f)
            horizontalLineToRelative(4.0f)
            verticalLineToRelative(1.0f)
            close()
            moveTo(17.25f, 18.0f)
            lineTo(6.75f, 18.0f)
            lineTo(6.75f, 4.0f)
            horizontalLineToRelative(10.5f)
            verticalLineToRelative(14.0f)
            close()
          }
        }
    return _phoneAndroid!!
  }

@Suppress("ObjectPropertyName") private var _phoneAndroid: ImageVector? = null
