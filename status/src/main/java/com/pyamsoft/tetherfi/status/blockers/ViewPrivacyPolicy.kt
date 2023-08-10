package com.pyamsoft.tetherfi.status.blockers

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import com.pyamsoft.pydroid.ui.uri.LocalExternalUriHandler
import com.pyamsoft.tetherfi.core.PRIVACY_POLICY_URL

private const val linkText = "Privacy Policy"
private const val uriTag = "privacy policy"

private inline fun AnnotatedString.Builder.withStringAnnotation(
    tag: String,
    annotation: String,
    content: () -> Unit
) {
  pushStringAnnotation(tag = tag, annotation = annotation)
  content()
  pop()
}

private fun onTextClicked(
    text: AnnotatedString,
    uriHandler: UriHandler,
    start: Int,
) {
  text
      .getStringAnnotations(
          tag = uriTag,
          start = start,
          end = start + linkText.length,
      )
      .firstOrNull()
      ?.also { uriHandler.openUri(it.item) }
}

@Composable
internal fun ViewPrivacyPolicy(
    modifier: Modifier = Modifier,
) {
  Box(
      modifier = modifier,
  ) {
    val text = buildAnnotatedString {
      append("View our ")

      withStringAnnotation(tag = uriTag, annotation = PRIVACY_POLICY_URL) {
        withStyle(
            style =
                SpanStyle(
                    textDecoration = TextDecoration.Underline,
                    color = MaterialTheme.colors.primary,
                ),
        ) {
          append(linkText)
        }
      }
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
        onClick = {
          onTextClicked(
              text = text,
              uriHandler = uriHandler,
              start = it,
          )
        },
    )
  }
}
