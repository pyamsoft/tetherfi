package com.pyamsoft.tetherfi.server.widi

import android.content.Context
import com.pyamsoft.tetherfi.server.ServerInternalApi
import com.pyamsoft.tetherfi.server.event.ConnectionEvent
import com.pyamsoft.tetherfi.server.event.ErrorEvent
import com.pyamsoft.tetherfi.server.permission.PermissionGuard
import com.pyamsoft.tetherfi.server.proxy.SharedProxy
import com.pyamsoft.tetherfi.server.status.RunningStatus
import com.pyamsoft.tetherfi.server.widi.receiver.WiDiReceiver
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import timber.log.Timber

@Singleton
internal class WiDiNetworkImpl
@Inject
internal constructor(
    @ServerInternalApi private val proxy: SharedProxy,
    @ServerInternalApi receiver: WiDiReceiver,
    @ServerInternalApi dispatcher: CoroutineDispatcher,
    @ServerInternalApi config: WiDiConfig,
    context: Context,
    permissionGuard: PermissionGuard,
    status: WiDiStatus,
) :
    ReceiverAwareWifiDirectNetwork(
        receiver,
        context,
        permissionGuard,
        dispatcher,
        config,
        status,
    ) {

  override suspend fun onNetworkStarted() {
    super.onNetworkStarted()
    Timber.d("Network started, start proxy")
    proxy.start()
  }

  override suspend fun onNetworkStopped() {
    super.onNetworkStopped()

    Timber.d("Stop proxy when wifi network removed")
    proxy.stop()
  }

  override suspend fun onErrorEvent(block: suspend (ErrorEvent) -> Unit) {
    return proxy.onErrorEvent(block)
  }

  override suspend fun onConnectionEvent(block: suspend (ConnectionEvent) -> Unit) {
    return proxy.onConnectionEvent(block)
  }

  override suspend fun onProxyStatusChanged(block: suspend (RunningStatus) -> Unit) {
    return proxy.onStatusChanged(block)
  }
}
