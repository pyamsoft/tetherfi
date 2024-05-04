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

package com.pyamsoft.tetherfi.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.pydroid.ui.uri.rememberUriHandler

private enum class LinkContentTypes {
  FAQ_LINK
}

fun LazyListScope.renderLinks(
    modifier: Modifier = Modifier,
    appName: String,
) {
  item(
      contentType = LinkContentTypes.FAQ_LINK,
  ) {
    val linkColor = MaterialTheme.colorScheme.primary

    val faqText = stringResource(R.string.faqs)
    val knownNotWorkingText = stringResource(R.string.known_to_not_work)
    val rawBlurb = stringResource(R.string.faq_blurb, appName, faqText, knownNotWorkingText)
    val faqBlurb =
        remember(
            linkColor,
            rawBlurb,
        ) {
          val faqIndex = rawBlurb.indexOf(faqText)
          val knwIndex = rawBlurb.indexOf(knownNotWorkingText)

          val linkStyle =
              SpanStyle(
                  color = linkColor,
                  textDecoration = TextDecoration.Underline,
              )

          val spanStyles =
              listOf(
                  AnnotatedString.Range(
                      linkStyle,
                      start = faqIndex,
                      end = faqIndex + faqText.length,
                  ),
                  AnnotatedString.Range(
                      linkStyle,
                      start = knwIndex,
                      end = knwIndex + knownNotWorkingText.length,
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
                // FAQ clickable
                addStringAnnotation(
                    tag = faqText,
                    annotation = "https://github.com/pyamsoft/tetherfi/wiki",
                    start = faqIndex,
                    end = faqIndex + faqText.length,
                )
                // KNW clickable
                addStringAnnotation(
                    tag = knownNotWorkingText,
                    annotation = "https://github.com/pyamsoft/tetherfi/wiki/Known-Not-Working",
                    start = knwIndex,
                    end = knwIndex + knownNotWorkingText.length,
                )
              }
              .toAnnotatedString()
        }

    Card(
        modifier = modifier.padding(vertical = MaterialTheme.keylines.content),
        border =
            BorderStroke(
                width = 2.dp,
                color = MaterialTheme.colorScheme.primaryContainer,
            ),
        shape = MaterialTheme.shapes.medium,
    ) {
      val uriHandler = rememberUriHandler()
      ClickableText(
          modifier = Modifier.fillMaxWidth().padding(MaterialTheme.keylines.content),
          text = faqBlurb,
          style =
              MaterialTheme.typography.bodyLarge.copy(
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
              ),
          onClick = { offset ->
            faqBlurb
                .getStringAnnotations(
                    tag = faqText,
                    start = offset,
                    end = offset + faqText.length,
                )
                .firstOrNull()
                ?.also { uriHandler.openUri(it.item) }

            faqBlurb
                .getStringAnnotations(
                    tag = knownNotWorkingText,
                    start = offset,
                    end = offset + knownNotWorkingText.length,
                )
                .firstOrNull()
                ?.also { uriHandler.openUri(it.item) }
          },
      )
    }
  }
}

@Preview
@Composable
private fun PreviewLinks() {
  LazyColumn {
    renderLinks(
        appName = "TEST",
    )
  }
}
