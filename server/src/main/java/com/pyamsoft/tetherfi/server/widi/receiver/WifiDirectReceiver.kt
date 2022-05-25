package com.pyamsoft.tetherfi.server.widi.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import androidx.annotation.CheckResult
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.core.Enforcer
import com.pyamsoft.tetherfi.server.ServerInternalApi
import com.pyamsoft.tetherfi.server.event.OnShutdownEvent
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
    private val shutdownBus: EventBus<OnShutdownEvent>,
    @ServerInternalApi private val eventBus: EventBus<WidiNetworkEvent>,
) : BroadcastReceiver(), WiDiReceiver {

  private val scope by lazy { CoroutineScope(context = Dispatchers.IO) }

  private var registered = false

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

  private suspend fun handleStateChangedAction(intent: Intent) {
    when (val p2pState = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, 0)) {
      WifiP2pManager.WIFI_P2P_STATE_ENABLED -> {
        Timber.d("Wifi P2P Enabled")
        eventBus.send(WidiNetworkEvent.WifiEnabled)
      }
      WifiP2pManager.WIFI_P2P_STATE_DISABLED -> {
        Timber.d("Wifi P2P Disabled")
        eventBus.send(WidiNetworkEvent.WifiDisabled)
        shutdownBus.send(OnShutdownEvent)
      }
      else -> Timber.w("Unknown Wifi p2p state: $p2pState")
    }
  }

  private suspend fun handleConnectionChangedAction(intent: Intent) {
    val ip = resolveWifiGroupIp(intent)
    eventBus.send(WidiNetworkEvent.ConnectionChanged(ip = ip))
  }

  private suspend fun handleDiscoveryChangedAction(intent: Intent) {
    eventBus.send(WidiNetworkEvent.DiscoveryChanged)
  }

  private suspend fun handlePeersChangedAction(intent: Intent) {
    Timber.d("Peers changed!")
    eventBus.send(WidiNetworkEvent.PeersChanged)
  }

  private suspend fun handleThisDeviceChangedAction(intent: Intent) {
    Timber.d("This Device changed!")
    eventBus.send(WidiNetworkEvent.ThisDeviceChanged)
  }

  override suspend fun onEvent(onEvent: suspend (WidiNetworkEvent) -> Unit) {
    return eventBus.onEvent(onEvent)
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
    // Go async in case scope work takes a long time
    val pending = goAsync()
    scope.launch(context = Dispatchers.IO) {
      Enforcer.assertOffMainThread()
      try {
        when (val action = intent.action) {
          WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> handleStateChangedAction(intent)
          WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> handleConnectionChangedAction(intent)
          WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION -> handleDiscoveryChangedAction(intent)
          WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> handlePeersChangedAction(intent)
          WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION ->
              handleThisDeviceChangedAction(intent)
          else -> {
            Timber.w("Unhandled intent action: $action")
          }
        }
      } finally {
        withContext(context = Dispatchers.Main) {
          Enforcer.assertOnMainThread()
          // Mark BR as finished
          pending.finish()
        }
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
