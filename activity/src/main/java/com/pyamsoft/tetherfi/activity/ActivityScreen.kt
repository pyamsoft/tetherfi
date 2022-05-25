package com.pyamsoft.tetherfi.activity

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
fun ActivityScreen(
    modifier: Modifier = Modifier,
    state: ActivityViewState,
    imageLoader: ImageLoader,
) {
  val events = state.events

  val scaffoldState = rememberScaffoldState()

  Scaffold(
      modifier = modifier,
      scaffoldState = scaffoldState,
  ) {
    Column(
        modifier = Modifier.padding(MaterialTheme.keylines.content),
    ) {
      Text(
          text = "Activity Log",
          style = MaterialTheme.typography.h5,
      )

      if (events.isEmpty()) {
        EmptyActivityScreen(
            imageLoader = imageLoader,
            bottomContent = {
              Text(
                  modifier = Modifier.padding(horizontal = MaterialTheme.keylines.content),
                  text = "No activity yet, connect a device!",
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
            )
          }
        }
      }
    }
  }
}
