package com.pyamsoft.widefi.server.widi.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pManager
import com.pyamsoft.pydroid.bus.EventBus
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

  private var registered = false

  private fun handleStateChangedAction(intent: Intent) {
    scope.launch(context = Dispatchers.IO) {
      when (val p2pState = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, 0)) {
        WifiP2pManager.WIFI_P2P_STATE_ENABLED -> {
          Timber.d("Wifi P2P Enabled")
          eventBus.send(WidiNetworkEvent.WifiEnabled)
        }
        WifiP2pManager.WIFI_P2P_STATE_DISABLED -> {
          Timber.d("Wifi P2P Disabled")
          eventBus.send(WidiNetworkEvent.WifiDisabled)
        }
        else -> Timber.w("Unknown Wifi p2p state: $p2pState")
      }
    }
  }

  override suspend fun onEvent(onEvent: (WidiNetworkEvent) -> Unit) {
    return eventBus.onEvent { onEvent(it) }
  }

  override suspend fun register() {
    val self = this
    withContext(context = Dispatchers.Main) {
      if (!registered) {
        registered = true
        context.registerReceiver(self, INTENT_FILTER)
      }
    }
  }

  override suspend fun unregister() {
    val self = this
    withContext(context = Dispatchers.Main) {
      if (registered) {
        registered = false
        context.unregisterReceiver(self)
      }
    }
  }

  override fun onReceive(context: Context, intent: Intent) {
    when (val action = intent.action) {
      WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> handleStateChangedAction(intent)
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
