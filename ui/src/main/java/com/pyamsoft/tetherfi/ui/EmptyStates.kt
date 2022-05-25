package com.pyamsoft.tetherfi.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import com.pyamsoft.pydroid.theme.keylines

private val MIN_IMAGE_HEIGHT = 120.dp

@Composable
fun BlankScreen(
    modifier: Modifier = Modifier,
    imageLoader: ImageLoader,
    @DrawableRes image: Int,
    illustrationBy: String,
    illustrationLink: String,
    from: String,
    topContent: @Composable ColumnScope.() -> Unit = {},
    bottomContent: @Composable ColumnScope.() -> Unit = {},
) {

  Column(
      modifier = modifier,
      horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    topContent()

    AsyncImage(
        modifier = Modifier.fillMaxWidth().heightIn(min = MIN_IMAGE_HEIGHT),
        model = image,
        imageLoader = imageLoader,
        contentScale = ContentScale.FillWidth,
        contentDescription = null,
    )
    Icons8RequiredAttribution(
        modifier =
            Modifier.padding(horizontal = MaterialTheme.keylines.content)
                .padding(bottom = MaterialTheme.keylines.content),
        illustrationBy = illustrationBy,
        illustrationLink = illustrationLink,
        from = from,
        fromLink = "https://icons8.com/illustrations",
    )

    bottomContent()
  }
}
