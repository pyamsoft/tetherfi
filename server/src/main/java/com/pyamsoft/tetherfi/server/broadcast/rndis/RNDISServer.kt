/*
 * Copyright 2024 pyamsoft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pyamsoft.tetherfi.server.broadcast.rndis

import android.annotation.SuppressLint
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.core.ThreadEnforcer
import com.pyamsoft.tetherfi.core.AppDevEnvironment
import com.pyamsoft.tetherfi.core.InAppRatingPreferences
import com.pyamsoft.tetherfi.core.Timber
import com.pyamsoft.tetherfi.server.ServerInternalApi
import com.pyamsoft.tetherfi.server.broadcast.BroadcastNetworkStatus
import com.pyamsoft.tetherfi.server.broadcast.BroadcastServer
import com.pyamsoft.tetherfi.server.broadcast.BroadcastStatus
import com.pyamsoft.tetherfi.server.event.ServerShutdownEvent
import com.pyamsoft.tetherfi.server.prereq.permission.PermissionGuard
import com.pyamsoft.tetherfi.server.proxy.SharedProxy
import com.pyamsoft.tetherfi.server.status.RunningStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class RNDISServer @Inject internal constructor(
    @ServerInternalApi private val proxy: SharedProxy,
    private val inAppRatingPreferences: InAppRatingPreferences,
    appEnvironment: AppDevEnvironment,
    enforcer: ThreadEnforcer,
    shutdownBus: EventBus<ServerShutdownEvent>,
    permissionGuard: PermissionGuard,
    clock: Clock,
    status: BroadcastStatus,
) : BroadcastServer<String>(
    shutdownBus,
    permissionGuard,
    appEnvironment,
    enforcer,
    clock,
    status,
) {

    override suspend fun withLockStartBroadcast(): String {
        // TODO resolve the RNDIS IP address from user preferences
        return "192.168.whatisthis.1"
    }

    @SuppressLint("VisibleForTests")
    override suspend fun resolveCurrentConnectionInfo(source: String): BroadcastNetworkStatus.ConnectionInfo {
        return BroadcastNetworkStatus.ConnectionInfo.Connected(
            hostName = source,
        )
    }

    override suspend fun resolveCurrentGroupInfo(source: String): BroadcastNetworkStatus.GroupInfo {
        return useRNDISGroupInfo()
    }

    override suspend fun withLockStopBroadcast(source: String) {
        // TODO what do?
    }

    override fun CoroutineScope.onNetworkStarted(
        connectionStatus: Flow<BroadcastNetworkStatus.ConnectionInfo>
    ) {
        // Need to mark the network as running so that the Proxy network can start
        Timber.d { "RNDIS is fully set up!" }
        status.set(RunningStatus.Running)

        launch(context = Dispatchers.Default) { inAppRatingPreferences.markHotspotUsed() }

        launch(context = Dispatchers.Default) { proxy.start(connectionStatus) }
    }

    override fun onProxyStatusChanged(): Flow<RunningStatus> {
        return proxy.onStatusChanged()
    }

    override fun getCurrentProxyStatus(): RunningStatus {
        return proxy.getCurrentStatus()
    }
}