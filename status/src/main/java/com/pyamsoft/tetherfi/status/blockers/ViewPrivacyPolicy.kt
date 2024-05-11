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

package com.pyamsoft.tetherfi.status.blockers

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import com.pyamsoft.pydroid.ui.uri.rememberUriHandler
import com.pyamsoft.tetherfi.core.PRIVACY_POLICY_URL
import com.pyamsoft.tetherfi.status.R

@Composable
internal fun ViewPrivacyPolicy(
    modifier: Modifier = Modifier,
    appName: String,
) {
  Box(
      modifier = modifier,
  ) {
    val linkColor = MaterialTheme.colorScheme.primary

    val privacyText = stringResource(R.string.block_privacy_policy)
    val rawBlurb = stringResource(R.string.block_view_privacy_policy, appName, privacyText)
    val text =
        remember(
            linkColor,
            rawBlurb,
            privacyText,
        ) {
          val privacyIndex = rawBlurb.indexOf(privacyText)

          val linkStyle =
              SpanStyle(
                  color = linkColor,
                  textDecoration = TextDecoration.Underline,
              )

          val spanStyles =
              listOf(
                  AnnotatedString.Range(
                      linkStyle,
                      start = privacyIndex,
                      end = privacyIndex + privacyText.length,
                  ),
              )

          val visualString =
              AnnotatedString(
                  rawBlurb,
                  spanStyles = spanStyles,
              )

          // Can only add annotations to builders
          return@remember AnnotatedString.Builder(visualString)
              .apply {
                addStringAnnotation(
                    tag = privacyText,
                    annotation = PRIVACY_POLICY_URL,
                    start = privacyIndex,
                    end = privacyIndex + privacyText.length,
                )
              }
              .toAnnotatedString()
        }

    val uriHandler = rememberUriHandler()
    ClickableText(
        text = text,
        style =
            MaterialTheme.typography.bodySmall.copy(
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        onClick = { start ->
          text
              .getStringAnnotations(
                  tag = privacyText,
                  start = start,
                  end = start + privacyText.length,
              )
              .firstOrNull()
              ?.also { uriHandler.openUri(it.item) }
        },
    )
  }
}

@Preview
@Composable
private fun PreviewViewPrivacyPolicy9() {
  ViewPrivacyPolicy(
      appName = "TEST",
  )
}
