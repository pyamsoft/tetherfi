package com.pyamsoft.widefi.error

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.widefi.activity.ActivityEvent

@Composable
internal fun ErrorScreen(
    modifier: Modifier = Modifier,
    state: ErrorViewState,
) {
  val events = state.events

  Column(
      modifier = modifier.padding(MaterialTheme.keylines.content),
  ) {
    Text(
        text = "Error Log",
        style = MaterialTheme.typography.h6,
    )

    LazyColumn {
      items(
          items = events,
          key = { it.host },
      ) { event ->
        ErrorItem(
            event = event,
        )
      }
    }
  }
}

@Composable
private fun ErrorItem(modifier: Modifier = Modifier, event: ActivityEvent) {
  val host = event.host
  val connections = remember(event.tcpConnections) { event.tcpConnections.get() }

  val localAlpha = LocalContentAlpha.current
  val alpha = remember(localAlpha) { localAlpha - 0.2F }

  Column(
      modifier = modifier,
  ) {
    Text(
        text = host,
        style = MaterialTheme.typography.h1,
    )

    Column {
      for (connection in connections) {
        val (url, methodMap) = connection
        Column {
          Text(
              text = url,
              color = MaterialTheme.colors.error,
              style =
                  MaterialTheme.typography.body2.copy(
                      fontWeight = FontWeight.Bold,
                  ),
          )

          val methodList = remember(methodMap) { methodMap.toList() }

          for (methodRow in methodList) {
            val (method, count) = methodRow
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                  text = method,
                  color =
                      MaterialTheme.colors.error.copy(
                          alpha = alpha,
                      ),
                  style =
                      MaterialTheme.typography.body2.copy(
                          fontWeight = FontWeight.SemiBold,
                      ),
              )
              Text(
                  modifier = Modifier.padding(start = MaterialTheme.keylines.content),
                  color =
                      MaterialTheme.colors.error.copy(
                          alpha = alpha,
                      ),
                  text = "$count",
                  style = MaterialTheme.typography.body2,
              )
            }
          }
        }
      }
    }
  }
}
