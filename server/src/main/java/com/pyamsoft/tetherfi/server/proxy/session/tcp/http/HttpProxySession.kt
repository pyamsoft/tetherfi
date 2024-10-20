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

package com.pyamsoft.tetherfi.server.proxy.session.tcp.http

import com.pyamsoft.pydroid.core.ThreadEnforcer
import com.pyamsoft.tetherfi.server.ServerInternalApi
import com.pyamsoft.tetherfi.server.clients.AllowedClients
import com.pyamsoft.tetherfi.server.clients.BlockedClients
import com.pyamsoft.tetherfi.server.clients.ClientResolver
import com.pyamsoft.tetherfi.server.proxy.SocketTagger
import com.pyamsoft.tetherfi.server.proxy.session.tcp.TcpProxySession
import com.pyamsoft.tetherfi.server.proxy.session.tcp.TcpSessionTransport
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class HttpProxySession
@Inject
internal constructor(
    @ServerInternalApi transport: TcpSessionTransport<HttpProxyRequest>,
    socketTagger: SocketTagger,
    blockedClients: BlockedClients,
    clientResolver: ClientResolver,
    allowedClients: AllowedClients,
    enforcer: ThreadEnforcer,
) :
    TcpProxySession<HttpProxyRequest>(
        transport = transport,
        socketTagger = socketTagger,
        blockedClients = blockedClients,
        clientResolver = clientResolver,
        allowedClients = allowedClients,
        enforcer = enforcer,
    )
