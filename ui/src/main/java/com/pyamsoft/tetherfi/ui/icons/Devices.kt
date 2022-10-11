package com.pyamsoft.tetherfi.ui.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

@Suppress("unused")
val Icons.Filled.Devices: ImageVector
  get() {
    if (_devices != null) {
      return _devices!!
    }
    _devices =
        materialIcon(name = "Filled.Devices") {
          materialPath {
            moveTo(4.0f, 6.0f)
            horizontalLineToRelative(18.0f)
            lineTo(22.0f, 4.0f)
            lineTo(4.0f, 4.0f)
            curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
            verticalLineToRelative(11.0f)
            lineTo(0.0f, 17.0f)
            verticalLineToRelative(3.0f)
            horizontalLineToRelative(14.0f)
            verticalLineToRelative(-3.0f)
            lineTo(4.0f, 17.0f)
            lineTo(4.0f, 6.0f)
            close()
            moveTo(23.0f, 8.0f)
            horizontalLineToRelative(-6.0f)
            curveToRelative(-0.55f, 0.0f, -1.0f, 0.45f, -1.0f, 1.0f)
            verticalLineToRelative(10.0f)
            curveToRelative(0.0f, 0.55f, 0.45f, 1.0f, 1.0f, 1.0f)
            horizontalLineToRelative(6.0f)
            curveToRelative(0.55f, 0.0f, 1.0f, -0.45f, 1.0f, -1.0f)
            lineTo(24.0f, 9.0f)
            curveToRelative(0.0f, -0.55f, -0.45f, -1.0f, -1.0f, -1.0f)
            close()
            moveTo(22.0f, 17.0f)
            horizontalLineToRelative(-4.0f)
            verticalLineToRelative(-7.0f)
            horizontalLineToRelative(4.0f)
            verticalLineToRelative(7.0f)
            close()
          }
        }
    return _devices!!
  }

@Suppress("ObjectPropertyName") private var _devices: ImageVector? = null
