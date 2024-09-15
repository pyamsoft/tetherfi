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
import com.pyamsoft.tetherfi.server.broadcast.BroadcastNetworkStatus
import com.pyamsoft.tetherfi.server.broadcast.BroadcastServer
import com.pyamsoft.tetherfi.server.broadcast.BroadcastServerImplementation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class RNDISServer @Inject internal constructor(
) : BroadcastServerImplementation<String> {

    override suspend fun withLockStartBroadcast(updateNetworkInfo: suspend (String) -> BroadcastServer.UpdateResult): String {
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

    override fun onNetworkStarted(
        scope: CoroutineScope,
        connectionStatus: Flow<BroadcastNetworkStatus.ConnectionInfo>
    ) {
    }
}