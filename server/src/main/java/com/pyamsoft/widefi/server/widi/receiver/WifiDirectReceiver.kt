package com.pyamsoft.widefi.server.widi.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pManager
import androidx.annotation.CheckResult
import androidx.core.content.getSystemService
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.widefi.server.ServerInternalApi
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

@Singleton
internal class WifiDirectReceiver
@Inject
internal constructor(
  private val context: Context,
  @ServerInternalApi private val eventBus: EventBus<WidiNetworkEvent>,
) : BroadcastReceiver(), WiDiReceiver {

  private val scope by lazy { CoroutineScope(context = Dispatchers.IO) }

  private val wifiManager by lazy { context.getSystemService<WifiManager>().requireNotNull() }
  private var registered = true

  @CheckResult
  private suspend fun isWifiEnabled(): Boolean =
      withContext(context = Dispatchers.Main) {
        return@withContext wifiManager.isWifiEnabled
      }

  private fun handleStateChangedAction() {
    scope.launch(context = Dispatchers.IO) {
      val enabled = isWifiEnabled()
      Timber.d("Wifi P2P State Changed: ${if (enabled) "Enabled" else "Disabled"}")
      if (enabled) {
        eventBus.send(WidiNetworkEvent.WifiEnabled)
      } else {
        eventBus.send(WidiNetworkEvent.WifiDisabled)
      }
    }
  }

  override suspend fun onEvent(onEvent: (WidiNetworkEvent) -> Unit) {
    return eventBus.onEvent { onEvent(it) }
  }

  override fun register() {
    if (!registered) {
      registered = true
      context.registerReceiver(this, INTENT_FILTER)
    }
  }

  override fun unregister() {
    if (registered) {
      registered = false
      context.unregisterReceiver(this)
    }
  }

  override fun onReceive(context: Context, intent: Intent) {
    when (val action = intent.action) {
      WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> handleStateChangedAction()
      WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {}
      WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION -> {}
      WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {}
      WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {}
      else -> {
        Timber.w("Unhandled intent action: $action")
      }
    }
  }

  companion object {
    private val INTENT_FILTER =
        IntentFilter().apply {
          addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
          addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
          addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
          addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION)
          addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        }
  }
}
