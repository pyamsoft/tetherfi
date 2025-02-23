/*
 * Copyright 2025 pyamsoft
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

package com.pyamsoft.tetherfi.server.proxy.session.tcp.socks.five

import androidx.annotation.CheckResult
import com.pyamsoft.tetherfi.core.Timber
import com.pyamsoft.tetherfi.server.proxy.ServerDispatcher
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import kotlinx.coroutines.withContext
import kotlinx.io.bytestring.decodeToString

internal object AddressResolver {

  @CheckResult
  private suspend fun readDestinationAddress(
      serverDispatcher: ServerDispatcher,
      sourceOrByteReadChannel: SourceOrByteReadChannel,
      addressType: SOCKS5AddressType
  ): InetAddress =
      withContext(context = serverDispatcher.primary) {
        when (addressType) {
          SOCKS5AddressType.IPV4 -> {
            val data = sourceOrByteReadChannel.readByteArray(4)
            return@withContext Inet4Address.getByAddress(data)
          }

          SOCKS5AddressType.DOMAIN_NAME -> {
            val addressLength = sourceOrByteReadChannel.readByte().toInt()
            val data = sourceOrByteReadChannel.readByteString(addressLength)

            val addressByteString = data.decodeToString()
            if (addressLength == 1 && addressByteString == "0") {
              // PySocks delivers a random port with an address of "0"
              // SOCKS spec says we must fall back to 0 address
              return@withContext InetAddress.getByName("0.0.0.0")
            } else {
              return@withContext InetAddress.getByName(addressByteString)
            }
          }

          SOCKS5AddressType.IPV6 -> {
            val data = sourceOrByteReadChannel.readByteArray(16)
            return@withContext Inet6Address.getByAddress(data)
          }
        }
      }

  @CheckResult
  internal suspend fun resolvePacketDestination(
      serverDispatcher: ServerDispatcher,
      sourceOrByteReadChannel: SourceOrByteReadChannel,
      onInvalidAddressType: suspend (Byte) -> Unit,
      onInvalidDestinationAddress: suspend (SOCKS5AddressType) -> Unit,
  ): PacketDestination? {
    // Resolve Address Type
    val addressTypeByte = sourceOrByteReadChannel.readByte()
    val addressType = SOCKS5AddressType.fromAddressType(addressTypeByte)
    if (addressType == null) {
      onInvalidAddressType(addressTypeByte)
      return null
    }

    // Then address
    val destinationAddress =
        try {
          readDestinationAddress(
              serverDispatcher = serverDispatcher,
              sourceOrByteReadChannel = sourceOrByteReadChannel,
              addressType = addressType,
          )
        } catch (e: Throwable) {
          Timber.e(e) { "Unable to parse the destination address" }
          onInvalidDestinationAddress(addressType)
          return null
        }

    // A short max is 32767 but ports can go up to 65k
    // Sometimes the short value is negative, in that case, we
    // "fix" it by converting back to an unsigned number
    val destinationPort = sourceOrByteReadChannel.readShort().toUShort()
    return PacketDestination(
        address = destinationAddress,
        port = destinationPort,
        addressType = addressType,
    )
  }
}
