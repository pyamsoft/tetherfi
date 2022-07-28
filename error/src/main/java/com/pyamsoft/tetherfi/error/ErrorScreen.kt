package com.pyamsoft.tetherfi.error

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.tetherfi.ui.event.ProxyEventItem

@Composable
fun ErrorScreen(
    modifier: Modifier = Modifier,
    state: ErrorViewState,
    imageLoader: ImageLoader,
) {
  val events = state.events

  val scaffoldState = rememberScaffoldState()

  Scaffold(
      modifier = modifier,
      scaffoldState = scaffoldState,
  ) { pv ->
    Column(
        modifier = Modifier.padding(pv).padding(MaterialTheme.keylines.content),
    ) {
      Text(
          text = "Error Log",
          style = MaterialTheme.typography.h5,
      )

      if (events.isEmpty()) {
        EmptyErrorsScreen(
            imageLoader = imageLoader,
            bottomContent = {
              Text(
                  modifier = Modifier.padding(horizontal = MaterialTheme.keylines.content),
                  text = "No errors, everything looks good!",
                  style = MaterialTheme.typography.h6,
              )
            },
        )
      } else {
        LazyColumn {
          items(
              items = events,
              key = { it.host },
          ) { event ->
            ProxyEventItem(
                modifier = Modifier.padding(bottom = 8.dp),
                event = event,
                color = MaterialTheme.colors.error,
            )
          }
        }
      }
    }
  }
}
