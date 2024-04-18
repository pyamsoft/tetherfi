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

package com.pyamsoft.tetherfi.info.sections

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.pyamsoft.pydroid.theme.keylines

private enum class ConnectionCompleteContentTypes {
  SHARING,
  DONE,
}

internal fun LazyListScope.renderConnectionComplete(
    itemModifier: Modifier = Modifier,
    appName: String,
) {
  item(
      contentType = ConnectionCompleteContentTypes.SHARING,
  ) {
    ThisInstruction(
        modifier = itemModifier,
    ) {
      Text(
          text = "Your device should now be sharing its Internet connection!",
          style = MaterialTheme.typography.bodyLarge,
      )
    }
  }

  item(
      contentType = ConnectionCompleteContentTypes.DONE,
  ) {
    OtherInstruction(
        modifier = itemModifier.padding(top = MaterialTheme.keylines.content),
    ) {
      Text(
          text =
              "At this point, normal Internet browsing and email should work. If it does not, disconnect from the $appName Hotspot and double-check that you have entered the correct Network and Proxy settings.",
          style =
              MaterialTheme.typography.bodyMedium.copy(
                  color = MaterialTheme.colorScheme.onBackground,
              ),
      )
    }
  }
}

@Preview
@Composable
private fun PreviewConnectionComplete() {
  LazyColumn {
    renderConnectionComplete(
        appName = "TEST",
    )
  }
}
