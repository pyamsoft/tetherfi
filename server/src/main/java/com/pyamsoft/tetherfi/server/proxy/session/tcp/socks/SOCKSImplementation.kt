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

package com.pyamsoft.tetherfi.server.proxy.session.tcp.socks

import com.pyamsoft.tetherfi.server.broadcast.BroadcastNetworkStatus
import com.pyamsoft.tetherfi.server.clients.ByteTransferReport
import com.pyamsoft.tetherfi.server.clients.TetherClient
import com.pyamsoft.tetherfi.server.network.SocketBinder
import com.pyamsoft.tetherfi.server.proxy.ServerDispatcher
import com.pyamsoft.tetherfi.server.proxy.SocketTracker
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import kotlinx.coroutines.CoroutineScope

internal interface SOCKSImplementation<R : SOCKSImplementation.Responder> {

    suspend fun usingResponder(proxyOutput: ByteWriteChannel, block: suspend R.() -> Unit)

    suspend fun handleSocksCommand(
        scope: CoroutineScope,
        serverDispatcher: ServerDispatcher,
        socketTracker: SocketTracker,
        networkBinder: SocketBinder.NetworkBinder,
        proxyInput: ByteReadChannel,
        proxyOutput: ByteWriteChannel,
        connectionInfo: BroadcastNetworkStatus.ConnectionInfo.Connected,
        client: TetherClient,
        onReport: suspend (ByteTransferReport) -> Unit,
    )

    interface Responder
}