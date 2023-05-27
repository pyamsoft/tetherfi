/*
 * Copyright 2023 pyamsoft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pyamsoft.tetherfi.server.widi

import android.content.Context
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.core.ThreadEnforcer
import com.pyamsoft.tetherfi.core.AppDevEnvironment
import com.pyamsoft.tetherfi.core.InAppRatingPreferences
import com.pyamsoft.tetherfi.server.ServerInternalApi
import com.pyamsoft.tetherfi.server.event.ServerShutdownEvent
import com.pyamsoft.tetherfi.server.permission.PermissionGuard
import com.pyamsoft.tetherfi.server.proxy.SharedProxy
import com.pyamsoft.tetherfi.server.status.RunningStatus
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class WiDiNetworkImpl
@Inject
internal constructor(
    private val inAppRatingPreferences: InAppRatingPreferences,
    @ServerInternalApi private val proxy: SharedProxy,
    @ServerInternalApi config: WiDiConfig,
    enforcer: ThreadEnforcer,
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
        config,
        appEnvironment,
        enforcer,
        status,
    ) {

  override suspend fun onNetworkStarted(scope: CoroutineScope) {
    proxy.start()

    inAppRatingPreferences.markHotspotUsed()
  }

  override suspend fun onNetworkStopped(scope: CoroutineScope) {
    proxy.stop()
  }

  override suspend fun onProxyStatusChanged(block: suspend (RunningStatus) -> Unit) {
    return proxy.onStatusChanged(block)
  }
}
