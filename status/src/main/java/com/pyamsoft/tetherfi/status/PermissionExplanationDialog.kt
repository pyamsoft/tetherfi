package com.pyamsoft.tetherfi.status

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.AlertDialog
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.tetherfi.core.PRIVACY_POLICY_URL

@Composable
internal fun PermissionExplanationDialog(
    modifier: Modifier = Modifier,
    state: StatusViewState,
    appName: String,
    onDismissPermissionExplanation: () -> Unit,
    onOpenPermissionSettings: () -> Unit,
    onRequestPermissions: () -> Unit,
) {
  AnimatedVisibility(
      modifier = modifier,
      visible = state.explainPermissions,
  ) {
    AlertDialog(
        onDismissRequest = onDismissPermissionExplanation,
        title = {
          Row(
              verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
                text = "Permission Request",
                style = MaterialTheme.typography.h5,
            )
          }
        },
        text = {
          Column {
            Text(
                text = "$appName needs PRECISE LOCATION permission to create a Wi-Fi Group",
                style = MaterialTheme.typography.body1,
            )

            Text(
                modifier = Modifier.padding(top = MaterialTheme.keylines.content),
                text =
                    "$appName will not use your location for anything else but Wi-Fi Group creation.",
                style = MaterialTheme.typography.body1,
            )

            ViewPrivacyPolicy(
                modifier = Modifier.padding(top = MaterialTheme.keylines.content),
            )
          }
        },
        buttons = {
          Row(
              modifier =
                  Modifier.padding(horizontal = MaterialTheme.keylines.content)
                      .padding(bottom = MaterialTheme.keylines.baseline),
              verticalAlignment = Alignment.CenterVertically,
          ) {
            TextButton(
                onClick = onOpenPermissionSettings,
                colors =
                    ButtonDefaults.textButtonColors(
                        contentColor =
                            MaterialTheme.colors.onSurface.copy(
                                alpha = ContentAlpha.medium,
                            ),
                    ),
            ) {
              Text(
                  text = "Open Settings",
              )
            }

            Spacer(
                modifier = Modifier.weight(1F),
            )

            TextButton(
                onClick = onDismissPermissionExplanation,
                colors =
                    ButtonDefaults.textButtonColors(
                        contentColor =
                            MaterialTheme.colors.error.copy(
                                alpha = ContentAlpha.medium,
                            ),
                    ),
            ) {
              Text(
                  text = "Deny",
              )
            }

            TextButton(
                onClick = onRequestPermissions,
            ) {
              Text(
                  text = "Grant",
              )
            }
          }
        },
    )
  }
}

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
private fun ViewPrivacyPolicy(
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

    val uriHandler = LocalUriHandler.current
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
