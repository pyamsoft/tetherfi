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
import com.pyamsoft.tetherfi.server.ServerInternalApi
import com.pyamsoft.tetherfi.server.proxy.session.ProxySession
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

internal class UdpProxySession
@Inject
internal constructor(
    @ServerInternalApi private val dispatcher: CoroutineDispatcher,
    private val enforcer: ThreadEnforcer,
) : ProxySession<UdpProxyData> {

  override suspend fun exchange(
      scope: CoroutineScope,
      data: UdpProxyData,
  ) =
      withContext(context = dispatcher) {
        enforcer.assertOffMainThread()

        val datagram = data.datagram
        Timber.d("Process UDP proxy data: ${datagram.address} ${datagram.packet}")
      }
}
