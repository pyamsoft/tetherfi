package com.pyamsoft.tetherfi.activity

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import coil.ImageLoader
import com.pyamsoft.tetherfi.ui.BlankScreen

@Composable
fun EmptyActivityScreen(
    modifier: Modifier = Modifier,
    imageLoader: ImageLoader,
    topContent: @Composable ColumnScope.() -> Unit = {},
    bottomContent: @Composable ColumnScope.() -> Unit = {},
) {
  BlankScreen(
      modifier = modifier,
      imageLoader = imageLoader,
      image = R.drawable.rocket_5g,
      illustrationBy = "AlexManokhi",
      illustrationLink = "https://icons8.com/illustrations/author/VKgWUPlqQ7Ea",
      from = "Ouch!",
      bottomContent = bottomContent,
      topContent = topContent,
  )
}
