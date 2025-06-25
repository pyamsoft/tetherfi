/*
 * Copyright 2025 pyamsoft
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

package com.pyamsoft.tetherfi.connections.sections

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.pydroid.ui.defaults.TypographyDefaults
import com.pyamsoft.tetherfi.connections.R

private enum class RenderExcuseContentTypes {
  SORRY,
  SORRY_EXTRA,
}

internal fun LazyListScope.renderExcuse(
    modifier: Modifier = Modifier,
) {
  item(
      contentType = RenderExcuseContentTypes.SORRY,
  ) {
    Text(
        modifier = modifier.padding(vertical = MaterialTheme.keylines.content),
        text = stringResource(R.string.connection_sorry),
        style =
            MaterialTheme.typography.bodyMedium.copy(
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = TypographyDefaults.ALPHA_DISABLED,
                    ),
            ),
        textAlign = TextAlign.Center,
    )
  }

  item(
      contentType = RenderExcuseContentTypes.SORRY_EXTRA,
  ) {
    Text(
        modifier = modifier.padding(vertical = MaterialTheme.keylines.content),
        text = stringResource(R.string.connection_excuse),
        style =
            MaterialTheme.typography.bodyMedium.copy(
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = TypographyDefaults.ALPHA_DISABLED,
                    ),
            ),
        textAlign = TextAlign.Center,
    )
  }
}
