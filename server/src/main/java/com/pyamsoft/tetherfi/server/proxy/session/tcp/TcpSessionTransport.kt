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

package com.pyamsoft.tetherfi.server.proxy.session.tcp

import androidx.annotation.CheckResult
import com.pyamsoft.tetherfi.server.clients.ByteTransferReport
import com.pyamsoft.tetherfi.server.clients.TetherClient
import com.pyamsoft.tetherfi.server.event.ProxyRequest
import com.pyamsoft.tetherfi.server.proxy.ServerDispatcher
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import kotlinx.coroutines.CoroutineScope

internal interface TcpSessionTransport {

  @CheckResult suspend fun parseRequest(input: ByteReadChannel): ProxyRequest?

  @CheckResult
  suspend fun exchangeInternet(
      scope: CoroutineScope,
      serverDispatcher: ServerDispatcher,
      proxyInput: ByteReadChannel,
      proxyOutput: ByteWriteChannel,
      internetInput: ByteReadChannel,
      internetOutput: ByteWriteChannel,
      request: ProxyRequest,
      client: TetherClient,
  ): ByteTransferReport?
}
