package com.pyamsoft.tetherfi.server.widi

import android.content.Context
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.tetherfi.core.AppDevEnvironment
import com.pyamsoft.tetherfi.server.ServerDefaults
import com.pyamsoft.tetherfi.server.ServerInternalApi
import com.pyamsoft.tetherfi.server.event.ServerShutdownEvent
import com.pyamsoft.tetherfi.server.permission.PermissionGuard
import com.pyamsoft.tetherfi.server.proxy.SharedProxy
import com.pyamsoft.tetherfi.server.status.RunningStatus
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher

@Singleton
internal class WiDiNetworkImpl
@Inject
internal constructor(
    @ServerInternalApi private val proxy: SharedProxy,
    @ServerInternalApi dispatcher: CoroutineDispatcher,
    @ServerInternalApi config: WiDiConfig,
    shutdownBus: EventBus<ServerShutdownEvent>,
    context: Context,
    permissionGuard: PermissionGuard,
    status: WiDiStatus,
    appEnvironment: AppDevEnvironment,
) :
    WifiDirectNetwork(
        shutdownBus,
        context,
        permissionGuard,
        dispatcher,
        config,
        appEnvironment,
        status,
    ) {

  override suspend fun onNetworkStarted() {
    proxy.start()
  }

  override suspend fun onNetworkStopped() {
    proxy.stop()
  }

  override suspend fun onProxyStatusChanged(block: suspend (RunningStatus) -> Unit) {
    return proxy.onStatusChanged(block)
  }
}
