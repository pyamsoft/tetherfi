package com.pyamsoft.tetherfi.ui

import androidx.annotation.CheckResult
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import timber.log.Timber

private const val EMPTY_ATTRIBUTION_TAG = ""

private data class Illustration(
    val text: AnnotatedString,
    val length: Int,
)

private inline fun AnnotatedString.Builder.withStringAnnotation(
    annotation: String,
    content: () -> Unit
) {
  pushStringAnnotation(
      tag = EMPTY_ATTRIBUTION_TAG,
      annotation = annotation,
  )
  content()
  pop()
}

/**
 * Icons8 requires us to attribute when we use their images
 *
 * https://icons8.com/license
 */
@Composable
fun Icons8RequiredAttribution(
    modifier: Modifier = Modifier,
    illustrationBy: String,
    illustrationLink: String,
    from: String,
    fromLink: String,
) {
  val uriHandler = LocalUriHandler.current
  val illustration =
      rememberAttributedText(
          illustrationBy = illustrationBy,
          illustrationLink = illustrationLink,
          from = from,
          fromLink = fromLink,
      )
  Box(
      modifier = modifier,
  ) {
    ClickableText(
        modifier = modifier,
        text = illustration.text,
        style =
            MaterialTheme.typography.caption.copy(
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.onSurface,
            ),
        onClick = { start ->
          onTextClicked(
              illustration = illustration,
              uriHandler = uriHandler,
              start = start,
          )
        },
    )
  }
}

@Composable
@CheckResult
private fun rememberAttributedText(
    illustrationBy: String,
    illustrationLink: String,
    from: String,
    fromLink: String,
): Illustration {
  val linkColor = MaterialTheme.colors.primary
  return remember(
      illustrationBy,
      illustrationLink,
      from,
      fromLink,
      linkColor,
  ) {
    val linkStyle =
        SpanStyle(
            textDecoration = TextDecoration.Underline,
            color = linkColor,
        )

    val annotated = buildAnnotatedString {
      // Illustration by "blah" from "blah"
      append("Illustration by ")
      withStringAnnotation(
          annotation = illustrationLink,
      ) {
        withStyle(
            style = linkStyle,
        ) { append(illustrationBy) }
      }

      append(" from ")
      withStringAnnotation(
          annotation = fromLink,
      ) {
        withStyle(
            style = linkStyle,
        ) { append(from) }
      }
    }
    return@remember Illustration(
        text = annotated,
        length = annotated.length,
    )
  }
}

private fun onTextClicked(
    uriHandler: UriHandler,
    illustration: Illustration,
    start: Int,
) {
  val url =
      illustration.text
          .getStringAnnotations(
              tag = EMPTY_ATTRIBUTION_TAG,
              start = start,
              end = illustration.length,
          )
          // Pick the first tag and assume it was clicked
          .firstOrNull()
  if (url == null) {
    Timber.w("Could not find clicked annotation at: $start ${illustration.text}")
  } else {
    Timber.d("Clicked url: ${url.item}")
    uriHandler.openUri(url.item)
  }
}

@Preview
@Composable
private fun PreviewIcons8RequiredAttribution() {
  Icons8RequiredAttribution(
      illustrationBy = "Testing",
      illustrationLink = "https://example.com",
      from = "Tester",
      fromLink = "https://example.com",
  )
}
