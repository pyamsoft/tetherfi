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

package com.pyamsoft.tetherfi.server.proxy.session.udp

import com.pyamsoft.pydroid.core.ThreadEnforcer
import com.pyamsoft.tetherfi.core.Timber
import com.pyamsoft.tetherfi.server.proxy.ServerDispatcher
import com.pyamsoft.tetherfi.server.proxy.session.ProxySession
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext

internal class UdpProxySession
@Inject
internal constructor(
    private val enforcer: ThreadEnforcer,
) : ProxySession<UdpProxyData> {

  override suspend fun exchange(
      scope: CoroutineScope,
      serverDispatcher: ServerDispatcher,
      data: UdpProxyData,
  ) =
      withContext(context = serverDispatcher.primary) {
        enforcer.assertOffMainThread()

        val datagram = data.datagram
        Timber.d { "Process UDP proxy data: ${datagram.address} ${datagram.packet}" }
      }
}
