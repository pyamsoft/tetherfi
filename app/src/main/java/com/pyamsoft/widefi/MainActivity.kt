package com.pyamsoft.widefi

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.pydroid.inject.Injector
import com.pyamsoft.pydroid.theme.keylines
import com.pyamsoft.pydroid.ui.app.PYDroidActivity
import com.pyamsoft.pydroid.ui.changelog.buildChangeLog
import com.pyamsoft.widefi.server.ErrorEvent
import com.pyamsoft.widefi.server.status.RunningStatus
import com.pyamsoft.widefi.server.widi.WiDiNetwork
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class MainActivity : PYDroidActivity() {

  @Inject @JvmField internal var wiDiNetwork: WiDiNetwork? = null

  override val applicationIcon = R.mipmap.ic_launcher_round

  override val changelog = buildChangeLog {}

  private inline fun toggleWiDi(
      on: Boolean,
      crossinline onInfo: (WiDiNetwork.GroupInfo) -> Unit,
  ) {
    val p = wiDiNetwork.requireNotNull()
    lifecycleScope.launch(context = Dispatchers.Main) {
      if (on) {
        p.stop()
      } else {
        p.start()
      }

      val info = p.getGroupInfo()
      if (info != null) {
        onInfo(info)
      }
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    Injector.obtainFromApplication<WidefiComponent>(this).inject(this)

    setContent {
      val proxyErrors = remember { mutableStateListOf<ErrorEvent>() }
      var isWiDiOn by remember { mutableStateOf(false) }

      var ssid by remember { mutableStateOf("") }
      var password by remember { mutableStateOf("") }

      var proxyStatus by remember { mutableStateOf<RunningStatus>(RunningStatus.NotRunning) }
      var widiStatus by remember { mutableStateOf<RunningStatus>(RunningStatus.NotRunning) }

      LaunchedEffect(true) {
        wiDiNetwork.requireNotNull().also { widi ->
          this.launch(context = Dispatchers.Main) { widi.onStatusChanged { widiStatus = it } }

          this.launch(context = Dispatchers.Main) { widi.onStatusChanged { widiStatus = it } }

          this.launch(context = Dispatchers.Main) { widi.onProxyStatusChanged { proxyStatus = it } }

          this.launch(context = Dispatchers.Main) { widi.onErrorEvent { proxyErrors.add(it) } }
        }
      }

      Scaffold {
        Column(
            modifier = Modifier.padding(MaterialTheme.keylines.content),
        ) {
          Button(
              onClick = {
                toggleWiDi(isWiDiOn) { info ->
                  ssid = info.ssid
                  password = info.password
                }
                isWiDiOn = !isWiDiOn
              },
          ) {
            Text(
                text = "Turn WideFi: ${if (isWiDiOn) "OFF" else "ON"}",
            )
          }

          Text(
              text = "WideFi Network: SSID=$ssid PASSWORD=$password",
              style = MaterialTheme.typography.body1,
          )

          Row(
              verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
                text = "WiFi Network Status:",
                style = MaterialTheme.typography.body2,
            )
            DisplayStatus(
                status = widiStatus,
            )
          }

          Row(
              verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
                text = "Proxy Status:",
                style = MaterialTheme.typography.body2,
            )
            DisplayStatus(
                status = proxyStatus,
            )
          }

          LazyColumn(
              verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            itemsIndexed(
                items = proxyErrors,
                key = { i, _ -> i },
            ) { _, e ->
              Column {
                when (e) {
                  is ErrorEvent.Tcp -> {
                    Text(
                        text = e.request?.host ?: "NO HOST",
                        style =
                            MaterialTheme.typography.body2.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                    )
                    Text(
                        text = e.throwable.message ?: "TCP Proxy Error",
                        style = MaterialTheme.typography.caption,
                    )
                    Text(
                        text = "${e.type.name}: ${e.request}",
                        style = MaterialTheme.typography.caption,
                    )
                  }
                  is ErrorEvent.Udp -> {
                    Text(
                        text = e.throwable.message ?: "UDP Proxy Error",
                        style = MaterialTheme.typography.caption,
                    )
                    Text(
                        text = e.type.name,
                        style = MaterialTheme.typography.caption,
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  override fun onDestroy() {
    super.onDestroy()

    lifecycleScope.launch(context = Dispatchers.Main) {
      wiDiNetwork?.stop()
      wiDiNetwork = null
    }
  }
}

@Composable
private fun DisplayStatus(status: RunningStatus) {
  val text =
      remember(status) {
        when (status) {
          is RunningStatus.Error -> "Error: ${status.message}"
          is RunningStatus.NotRunning -> "Not Running"
          is RunningStatus.Running -> "Running"
          is RunningStatus.Starting -> "Starting"
          is RunningStatus.Stopping -> "Stopping"
        }
      }

  val color =
      remember(status) {
        when (status) {
          is RunningStatus.Error -> Color.Red
          is RunningStatus.NotRunning -> Color.Unspecified
          is RunningStatus.Running -> Color.Green
          is RunningStatus.Starting -> Color.Cyan
          is RunningStatus.Stopping -> Color.Magenta
        }
      }

  Text(
      text = text,
      style = MaterialTheme.typography.body2,
      color = color,
  )
}
