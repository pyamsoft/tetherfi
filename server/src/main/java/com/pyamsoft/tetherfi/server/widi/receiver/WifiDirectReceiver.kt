package com.pyamsoft.tetherfi.server.widi.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import androidx.annotation.CheckResult
import androidx.core.content.ContextCompat
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
) : BroadcastReceiver(), WiDiReceiver, WiDiReceiverRegister {

  private val scope by lazy { CoroutineScope(context = Dispatchers.IO) }

  private var registered = false

  @CheckResult
  private fun resolveWifiGroupIp(intent: Intent): String {
    val p2pInfo = getWifiP2PInfo(intent)
    if (p2pInfo == null) {
      Timber.w("No P2P Info in connection intent")
      return ""
    }

    val address = p2pInfo.groupOwnerAddress
    if (address == null) {
      Timber.w("No Group owner address in connection intent")
      return ""
    }

    return address.hostAddress.orEmpty()
  }

  private suspend fun handleStateChangedAction(intent: Intent) {
    when (val p2pState = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, 0)) {
      WifiP2pManager.WIFI_P2P_STATE_ENABLED -> {
        Timber.d("WiFi Direct: Enabled")
        eventBus.send(WidiNetworkEvent.WifiEnabled)
      }
      WifiP2pManager.WIFI_P2P_STATE_DISABLED -> {
        Timber.d("WiFi Direct: Disabled")
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
    eventBus.send(WidiNetworkEvent.PeersChanged)
  }

  private suspend fun handleThisDeviceChangedAction(intent: Intent) {
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
        ContextCompat.registerReceiver(
            context,
            self,
            INTENT_FILTER,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
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

    @CheckResult
    @Suppress("DEPRECATION")
    private fun getWifiP2PInfo(intent: Intent): WifiP2pInfo? {
      return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
          intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_INFO, WifiP2pInfo::class.java)
      else intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_INFO)
    }

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
