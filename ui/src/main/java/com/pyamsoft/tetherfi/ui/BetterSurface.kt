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

package com.pyamsoft.tetherfi.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pyamsoft.pydroid.ui.util.surfaceColorAtElevation

/**
 * In Light Mode, surfaces at the same elevation have a very thin line between them
 *
 * We can work around that with this Composable BUT, it will not have the surface's Shadows
 */
@Composable
fun BetterSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    color: Color = MaterialTheme.colors.surface,
    contentColor: Color = contentColorFor(color),
    border: BorderStroke? = null,
    elevation: Dp = 0.dp,
    content: @Composable () -> Unit
) {
  CompositionLocalProvider(
      LocalContentColor provides contentColor,
      //      val absoluteElevation = LocalAbsoluteElevation.current + elevation
      //      LocalAbsoluteElevation provides absoluteElevation,
  ) {
    Box(
        modifier =
            modifier
                .background(
                    color =
                        surfaceColorAtElevation(
                            elevation = elevation,
                        ),
                    shape = shape,
                )
                .run {
                  if (border != null) {
                    border(
                        border = border,
                        shape = shape,
                    )
                  } else {
                    this
                  }
                },
    ) {
      content()
    }
  }
}
