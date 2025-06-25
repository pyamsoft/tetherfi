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

package com.pyamsoft.tetherfi.main.blockers

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import com.pyamsoft.pydroid.ui.uri.rememberUriHandler
import com.pyamsoft.tetherfi.core.PRIVACY_POLICY_URL
import com.pyamsoft.tetherfi.main.R

@Composable
internal fun ViewPrivacyPolicy(
    modifier: Modifier = Modifier,
    appName: String,
) {
  val uriHandler = rememberUriHandler()
  val handleLinkClicked by rememberUpdatedState { link: LinkAnnotation ->
    if (link is LinkAnnotation.Url) {
      uriHandler.openUri(link.url)
    }
  }

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
                addLink(
                    url =
                        LinkAnnotation.Url(
                            url = PRIVACY_POLICY_URL,
                            linkInteractionListener = { handleLinkClicked(it) },
                        ),
                    start = privacyIndex,
                    end = privacyIndex + privacyText.length,
                )
              }
              .toAnnotatedString()
        }

    Text(
        text = text,
        style =
            MaterialTheme.typography.bodySmall.copy(
                textAlign = TextAlign.Center,
            ),
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
