package com.pyamsoft.tetherfi.ui.event

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pyamsoft.pydroid.theme.keylines

@Composable
fun ProxyEventItem(
    modifier: Modifier = Modifier,
    event: ProxyEvent,
    color: Color = Color.Unspecified,
) {
  val host = event.host
  val connections = remember(event.tcpConnections) { event.tcpConnections.get() }

  Column(
      modifier = modifier,
  ) {
    Text(
        text = host,
        style =
            MaterialTheme.typography.h6.copy(
                color = color,
            ),
    )

    Column {
      for (c in connections) {
        val url = c.first
        val methodMap = c.second
        ProxyEventItemUrl(
            modifier = Modifier.padding(start = 16.dp),
            url = url,
            methodMap = methodMap,
            color = color,
        )
      }
    }
  }
}

@Composable
private fun ProxyEventItemUrl(
    modifier: Modifier = Modifier,
    url: String,
    methodMap: Map<String, Int>,
    color: Color,
) {
  val methodList = remember(methodMap) { methodMap.toList() }

  Column(
      modifier = modifier,
  ) {
    Text(
        text = url,
        style =
            MaterialTheme.typography.body1.copy(
                fontWeight = FontWeight.Bold,
                color = color,
            ),
    )

    for (row in methodList) {
      val method = row.first
      val count = row.second
      ProxyEventItemMethod(
          modifier = Modifier.padding(start = 16.dp),
          method = method,
          count = count,
          color = color,
      )
    }
  }
}

@Composable
private fun ProxyEventItemMethod(
    modifier: Modifier = Modifier,
    method: String,
    count: Int,
    color: Color,
) {

  val style =
      MaterialTheme.typography.caption.copy(
          color = color,
      )

  Row(
      modifier = modifier,
      verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
        text = method,
        style = style,
    )
    Text(
        modifier = Modifier.padding(start = MaterialTheme.keylines.content),
        text = "$count",
        style = style,
    )
  }
}
