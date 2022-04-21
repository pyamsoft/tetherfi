package com.pyamsoft.widefi.activity

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.widefi.ui.event.ProxyEventItem

@Composable
fun ActivityScreen(
    modifier: Modifier = Modifier,
    state: ActivityViewState,
) {
  val events = state.events

  Column(
      modifier = modifier.padding(MaterialTheme.keylines.content),
  ) {
    Text(
        text = "Activity Log",
        style = MaterialTheme.typography.h5,
    )

    LazyColumn {
      items(
          items = events,
          key = { it.host },
      ) { event ->
        ProxyEventItem(
            event = event,
        )
      }
    }
  }
}
