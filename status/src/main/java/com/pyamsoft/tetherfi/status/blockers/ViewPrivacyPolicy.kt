package com.pyamsoft.tetherfi.status.blockers

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import com.pyamsoft.pydroid.ui.uri.LocalExternalUriHandler
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
          linkColor = MaterialTheme.colors.primary,
          text = LINK_TEXT,
          url = PRIVACY_POLICY_URL,
      )
    }

    val uriHandler = LocalExternalUriHandler.current
    ClickableText(
        text = text,
        style =
            MaterialTheme.typography.caption.copy(
                textAlign = TextAlign.Center,
                color =
                    MaterialTheme.colors.onSurface.copy(
                        alpha = ContentAlpha.medium,
                    ),
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
