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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.pyamsoft.pydroid.ui.uri.rememberUriHandler
import com.pyamsoft.tetherfi.core.PRIVACY_POLICY_URL
import com.pyamsoft.tetherfi.ui.appendLink

private const val LINK_TEXT = "Privacy Policy"
private const val URI_TAG = "privacy policy"

@Composable
internal fun ViewPrivacyPolicy(
    modifier: Modifier = Modifier,
) {
  Box(
      modifier = modifier,
  ) {
    val text = buildAnnotatedString {
      append("View our ")

      appendLink(
          tag = URI_TAG,
          linkColor = MaterialTheme.colorScheme.primary,
          text = LINK_TEXT,
          url = PRIVACY_POLICY_URL,
      )
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
                  tag = URI_TAG,
                  start = start,
                  end = start + LINK_TEXT.length,
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
  ViewPrivacyPolicy()
}
