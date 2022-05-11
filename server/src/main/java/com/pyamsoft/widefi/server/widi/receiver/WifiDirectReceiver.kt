package com.pyamsoft.widefi.server.widi.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pGroup
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import androidx.annotation.CheckResult
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

  @CheckResult
  private fun resolveWifiGroupIp(intent: Intent): String {
    val p2pInfo: WifiP2pInfo? = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_INFO)
    if (p2pInfo == null) {
      Timber.w("No P2P Info in connection intent")
      return ""
    }

    val address = p2pInfo.groupOwnerAddress
    if (address == null) {
      Timber.w("No Group owner address in connection intent")
      return ""
    }

    val ip = address.hostAddress.orEmpty()
    Timber.d("Host address: $ip")
    return ip
  }

  private fun handleConnectionChangedAction(intent: Intent) {
    scope.launch(context = Dispatchers.IO) {
      val ip = resolveWifiGroupIp(intent)
      eventBus.send(WidiNetworkEvent.ConnectionChanged(ip = ip))
    }
  }

  private fun handleDiscoveryChangedAction(intent: Intent) {
    scope.launch(context = Dispatchers.IO) {
      eventBus.send(WidiNetworkEvent.DiscoveryChanged)
    }
  }

  private fun handlePeersChangedAction(intent: Intent) {
    scope.launch(context = Dispatchers.IO) {
      Timber.d("Peers changed!")
      eventBus.send(WidiNetworkEvent.PeersChanged)
    }
  }

  private fun handleThisDeviceChangedAction(intent: Intent) {
    scope.launch(context = Dispatchers.IO) {
      Timber.d("This Device changed!")
      eventBus.send(WidiNetworkEvent.ThisDeviceChanged)
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
      WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> handleConnectionChangedAction(intent)
      WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION -> handleDiscoveryChangedAction(intent)
      WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> handlePeersChangedAction(intent)
      WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> handleThisDeviceChangedAction(intent)
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
