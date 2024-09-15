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
import androidx.annotation.CheckResult
import com.pyamsoft.tetherfi.server.broadcast.BroadcastNetworkStatus
import com.pyamsoft.tetherfi.server.broadcast.BroadcastServerImplementation
import com.pyamsoft.tetherfi.server.broadcast.DelegatingBroadcastServer
import java.net.NetworkInterface
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

@Singleton
internal class RNDISServer @Inject internal constructor() : BroadcastServerImplementation<String> {

  @CheckResult
  private suspend fun resolveRNDISNetwork(): String =
      withContext(context = Dispatchers.IO) {
        val allIfaces = NetworkInterface.getNetworkInterfaces()
        for (iface in allIfaces) {
          if (iface.name.orEmpty().lowercase().startsWith(EXPECTED_RNDIS_NAME_PREFIX)) {
            for (address in iface.inetAddresses) {
              val hostName = address.hostName.orEmpty()
              if (hostName.startsWith(EXPECTED_RNDIS_IP_PREFIX)) {
                return@withContext hostName
              }
            }
          }
        }

        // Couldn't find RNDIS
        throw IllegalStateException("Could not find USB Tethering connection")
      }

  override suspend fun withLockStartBroadcast(
      updateNetworkInfo: suspend (String) -> DelegatingBroadcastServer.UpdateResult
  ): String {
    return resolveRNDISNetwork()
  }

  @SuppressLint("VisibleForTests")
  override suspend fun resolveCurrentConnectionInfo(
      source: String
  ): BroadcastNetworkStatus.ConnectionInfo {
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
  ) {}

  companion object {
    private const val EXPECTED_RNDIS_NAME_PREFIX = "rndis"
    private const val EXPECTED_RNDIS_IP_PREFIX = "192.168."
  }
}
