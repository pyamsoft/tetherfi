package com.pyamsoft.tetherfi.server.widi

import android.content.Context
import androidx.annotation.CallSuper
import com.pyamsoft.tetherfi.server.permission.PermissionGuard
import com.pyamsoft.tetherfi.server.widi.receiver.WiDiReceiver
import com.pyamsoft.tetherfi.server.widi.receiver.WidiNetworkEvent
import kotlinx.coroutines.CoroutineDispatcher
import timber.log.Timber

internal abstract class ReceiverAwareWifiDirectNetwork
protected constructor(
    private val receiver: WiDiReceiver,
    context: Context,
    permissionGuard: PermissionGuard,
    dispatcher: CoroutineDispatcher,
    config: WiDiConfig,
    status: WiDiStatus,
) :
    WifiDirectNetwork(
        context,
        permissionGuard,
        dispatcher,
        config,
        status,
    ) {

  @CallSuper
  override suspend fun onNetworkStarted() {
    Timber.d("Register wifi receiver")
    receiver.register()
  }

  @CallSuper
  override suspend fun onNetworkStopped() {
    Timber.d("Unregister wifi receiver")
    receiver.unregister()
  }

  final override suspend fun onWifiDirectEvent(block: suspend (WidiNetworkEvent) -> Unit) {
    return receiver.onEvent { block(it) }
  }
}
