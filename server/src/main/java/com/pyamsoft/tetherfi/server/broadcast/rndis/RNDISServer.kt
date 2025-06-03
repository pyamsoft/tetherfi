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
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Enumeration
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
        // On some devices, this method does not return interfaces?
        // https://github.com/pyamsoft/tetherfi/issues/351
        val allIfaces: Enumeration<NetworkInterface>? = NetworkInterface.getNetworkInterfaces()
        if (allIfaces != null) {
          for (iface in allIfaces) {
            for (prefix in EXPECTED_RNDIS_NAME_PREFIX_LIST) {
              if (iface.name.orEmpty().lowercase().startsWith(prefix)) {
                for (address in iface.inetAddresses) {
                  val hostName = address.hostName.orEmpty()
                  for (ip in EXPECTED_RNDIS_IP_PREFIX_LIST) {
                    if (hostName.startsWith(ip)) {
                      return@withContext hostName
                    }
                  }
                }
              }
            }
          }
        }

        // Couldn't find RNDIS
        val allIfaceNames =
            allIfaces
                ?.asSequence()
                .orEmpty()
                .map { iface ->
                  return@map IfacePairing(
                      name = iface.name.orEmpty(),
                      address =
                          iface.inetAddresses
                              .asSequence()
                              .filter { it is Inet4Address }
                              .filterNot { it.isLoopbackAddress }
                              .map { it.hostName.orEmpty() }
                              .toList(),
                  )
                }
                .filter { it.name.isNotBlank() }
                .filter { it.address.isNotEmpty() }
                .toList()

        throw IllegalStateException("Missing USB Tethering connection. Tried=${allIfaceNames}")
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

  private data class IfacePairing(
      val name: String,
      val address: List<String>,
  )

  companion object {
    // Ordered by preferred interface prefix
    private val EXPECTED_RNDIS_NAME_PREFIX_LIST =
        arrayOf(
            // Samsung?
            "rndis",

            // https://github.com/pyamsoft/tetherfi/issues/351
            "ncm",

            // https://github.com/pyamsoft/tetherfi/issues/394#issuecomment-2930177923
            "usb",
        )
    private val EXPECTED_RNDIS_IP_PREFIX_LIST =
        arrayOf(
            // Samsung? and others?
            "192.168.",
        )
  }
}
