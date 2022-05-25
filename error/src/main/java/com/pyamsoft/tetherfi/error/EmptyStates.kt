package com.pyamsoft.tetherfi.error

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import coil.ImageLoader
import com.pyamsoft.tetherfi.ui.BlankScreen

@Composable
fun EmptyErrorsScreen(
    modifier: Modifier = Modifier,
    imageLoader: ImageLoader,
    topContent: @Composable ColumnScope.() -> Unit = {},
    bottomContent: @Composable ColumnScope.() -> Unit = {},
) {
  BlankScreen(
      modifier = modifier,
      imageLoader = imageLoader,
      image = R.drawable.internet_errors,
      illustrationBy = "Marina Green",
      illustrationLink = "https://icons8.com/illustrations/author/259416",
      from = "Ouch!",
      bottomContent = bottomContent,
      topContent = topContent,
  )
}
