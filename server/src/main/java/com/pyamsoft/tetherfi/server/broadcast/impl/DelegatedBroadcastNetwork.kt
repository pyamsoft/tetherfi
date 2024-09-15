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

package com.pyamsoft.tetherfi.server.broadcast.impl

import com.pyamsoft.tetherfi.server.broadcast.BroadcastNetwork
import com.pyamsoft.tetherfi.server.broadcast.BroadcastNetworkStatus
import com.pyamsoft.tetherfi.server.broadcast.BroadcastNetworkUpdater
import com.pyamsoft.tetherfi.server.broadcast.rndis.RNDISServer
import com.pyamsoft.tetherfi.server.broadcast.wifidirect.WifiDirectNetwork
import com.pyamsoft.tetherfi.server.status.RunningStatus
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DelegatedBroadcastNetwork @Inject internal constructor(
    private val wifiDirectNetwork: WifiDirectNetwork,
    private val rndisServer: RNDISServer,
): BroadcastNetwork, BroadcastNetworkStatus, BroadcastNetworkUpdater {

    override suspend fun start() {
        wifiDirectNetwork.start()
    }

    override fun onGroupInfoChanged(): Flow<BroadcastNetworkStatus.GroupInfo> {
        return wifiDirectNetwork.onGroupInfoChanged()
    }

    override fun onConnectionInfoChanged(): Flow<BroadcastNetworkStatus.ConnectionInfo> {
        return wifiDirectNetwork.onConnectionInfoChanged()
    }

    override fun getCurrentStatus(): RunningStatus {
        return wifiDirectNetwork.getCurrentStatus()
    }

    override fun onStatusChanged(): Flow<RunningStatus> {
        return wifiDirectNetwork.onStatusChanged()
    }

    override suspend fun updateNetworkInfo() {
        return wifiDirectNetwork.updateNetworkInfo()
    }
}