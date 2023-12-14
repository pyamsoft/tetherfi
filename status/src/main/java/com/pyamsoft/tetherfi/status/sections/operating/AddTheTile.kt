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

package com.pyamsoft.tetherfi.status.sections.operating

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.Card
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.pydroid.ui.defaults.CardDefaults
import com.pyamsoft.pydroid.ui.uri.rememberUriHandler
import com.pyamsoft.tetherfi.ui.appendLink

@Composable
private fun BugReportLink(
    modifier: Modifier = Modifier,
    isEditable: Boolean,
) {
  val highAlpha = if (isEditable) ContentAlpha.high else ContentAlpha.disabled
  val mediumAlpha = if (isEditable) ContentAlpha.medium else ContentAlpha.disabled

  val bugReportText = "this bug report."
  val textColor =
      MaterialTheme.colors.onSurface.copy(
          alpha = mediumAlpha,
      )
  val linkColor =
      MaterialTheme.colors.primary.copy(
          alpha = highAlpha,
      )
  val bugReportBlurb =
      remember(
          bugReportText,
          textColor,
          linkColor,
      ) {
        buildAnnotatedString {
          withStyle(
              style =
                  SpanStyle(
                      color = textColor,
                  ),
          ) {
            append("For further information, please see ")
            appendLink(
                tag = "bug",
                linkColor = linkColor,
                text = bugReportText,
                url = "https://github.com/pyamsoft/tetherfi/issues/250",
            )
          }
        }
      }

  val uriHandler = rememberUriHandler()
  ClickableText(
      modifier = modifier,
      text = bugReportBlurb,
      style =
          MaterialTheme.typography.caption.copy(
              color =
                  MaterialTheme.colors.onSurface.copy(
                      alpha = mediumAlpha,
                  ),
          ),
      onClick = { offset ->
        if (isEditable) {
          bugReportBlurb
              .getStringAnnotations(
                  tag = "bug",
                  start = offset,
                  end = offset + bugReportText.length,
              )
              .firstOrNull()
              ?.also { uriHandler.openUri(it.item) }
        }
      },
  )
}

@Composable
internal fun AddTheTile(
    modifier: Modifier = Modifier,
    isEditable: Boolean,
    appName: String,
) {
  val highAlpha = if (isEditable) ContentAlpha.high else ContentAlpha.disabled
  val mediumAlpha = if (isEditable) ContentAlpha.medium else ContentAlpha.disabled

  Card(
      modifier =
          modifier.border(
              width = 2.dp,
              color = MaterialTheme.colors.primary.copy(alpha = mediumAlpha),
              shape = MaterialTheme.shapes.medium,
          ),
      elevation = CardDefaults.Elevation,
  ) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
      Text(
          modifier =
              Modifier.fillMaxWidth()
                  .padding(horizontal = MaterialTheme.keylines.content)
                  .padding(top = MaterialTheme.keylines.content),
          text = "Elevated Importance",
          style =
              MaterialTheme.typography.h6.copy(
                  color =
                      MaterialTheme.colors.primary.copy(
                          alpha = highAlpha,
                      ),
              ),
      )

      Text(
          modifier = Modifier.fillMaxWidth().padding(MaterialTheme.keylines.content),
          text =
              "The Operating System can potentially slow down network performance when " +
                  "$appName is not the currently Active app on screen. This slow down" +
                  " can sometimes be mitigated by adding the $appName Tile to the " +
                  "Quick Settings Panel.",
          style =
              MaterialTheme.typography.caption.copy(
                  color =
                      MaterialTheme.colors.onSurface.copy(
                          alpha = mediumAlpha,
                      ),
              ),
      )
      Text(
          modifier =
              Modifier.fillMaxWidth()
                  .padding(horizontal = MaterialTheme.keylines.content)
                  .padding(bottom = MaterialTheme.keylines.baseline),
          // Really Android?
          text =
              """1. Swipe down to open the Quick Settings panel.
2. Tap the edit button.
3. Scroll through the tiles until you locate the $appName tile.
4. Drag the $appName tile to the list of active tiles.
            """
                  .trimIndent(),
          style =
              MaterialTheme.typography.caption.copy(
                  color =
                      MaterialTheme.colors.onSurface.copy(
                          alpha = mediumAlpha,
                      ),
              ),
      )

      QuickTileAddButton(
          modifier =
              Modifier.fillMaxWidth()
                  .padding(bottom = MaterialTheme.keylines.baseline)
                  .padding(horizontal = MaterialTheme.keylines.content),
          isEditable = isEditable,
          appName = appName,
      )

      BugReportLink(
          modifier =
              Modifier.fillMaxWidth()
                  .padding(bottom = MaterialTheme.keylines.content)
                  .padding(horizontal = MaterialTheme.keylines.content),
          isEditable = isEditable,
      )
    }
  }
}
