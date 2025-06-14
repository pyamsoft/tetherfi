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

package com.pyamsoft.tetherfi.server.clients

import androidx.annotation.CheckResult
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.pyamsoft.tetherfi.server.IPV4_ADDRESS_REGEX
import java.time.Clock
import java.time.LocalDateTime
import org.jetbrains.annotations.TestOnly

// TODO for performance and to avoid mass allocation, make all of these vals into vars
//      and manage the client list via flows.
//      Otherwise we allocate so frequently for time updates and for bandwidth updates
//      Instead we can just update variables, and only when the connection screen
//      is displayed we can periodically poll for updates.
@Stable
@Immutable
sealed class TetherClient(
    open val nickName: String,
    open val mostRecentlySeen: LocalDateTime,
    open val transferLimit: TransferAmount?,
    protected open val totalBytes: ByteTransferReport,

    // Bandwidth limit amount
    open val bandwidthLimit: TransferAmount?,
    // State tracker for the actual limit
    internal open val limiter: BandwidthLimiter,
) {

  val transferToInternet by lazy { TransferAmount.fromBytes(totalBytes.proxyToInternet) }
  val transferFromInternet by lazy { TransferAmount.fromBytes(totalBytes.internetToProxy) }

  @CheckResult
  fun matches(o: TetherClient): Boolean {
    when (this) {
      is IpAddressClient -> {
        if (o is IpAddressClient) {
          return ip == o.ip
        }

        return false
      }
      is HostNameClient -> {
        if (o is HostNameClient) {
          return hostname == o.hostname
        }

        return false
      }
    }
  }

  @CheckResult
  fun matches(hostNameOrIp: String): Boolean {
    when (this) {
      is IpAddressClient -> {
        if (IPV4_ADDRESS_REGEX.matches(hostNameOrIp)) {
          return ip == hostNameOrIp
        }

        return false
      }
      is HostNameClient -> {
        if (!IPV4_ADDRESS_REGEX.matches(hostNameOrIp)) {
          return hostname == hostNameOrIp
        }

        return false
      }
    }
  }

  @CheckResult
  fun isOverTransferLimit(): Boolean {
    // No limit, we are fine
    val l = transferLimit ?: return false

    // Our transfer unit is larger than our limit
    if (transferToInternet.bytes >= l.bytes) {
      return true
    }

    if (transferFromInternet.bytes >= l.bytes) {
      return true
    }

    return false
  }

  @CheckResult
  fun mergeReport(report: ByteTransferReport): ByteTransferReport {
    return report.copy(
        internetToProxy = report.internetToProxy + totalBytes.internetToProxy,
        proxyToInternet = report.proxyToInternet + totalBytes.proxyToInternet,
    )
  }

  companion object {

    @CheckResult
    private fun create(
        hostNameOrIp: String,
        clock: Clock,
        nickName: String,
        transferLimit: TransferAmount?,
        bandwidthLimit: TransferAmount?,
        totalBytes: ByteTransferReport,
    ): TetherClient {
      return if (IPV4_ADDRESS_REGEX.matches(hostNameOrIp)) {
        IpAddressClient(
            ip = hostNameOrIp,
            mostRecentlySeen = LocalDateTime.now(clock),
            nickName = nickName,
            // Transfer
            transferLimit = transferLimit,
            totalBytes = totalBytes,
            // Bandwidth
            bandwidthLimit = bandwidthLimit,
            limiter = BandwidthLimiter(),
        )
      } else {
        HostNameClient(
            hostname = hostNameOrIp,
            mostRecentlySeen = LocalDateTime.now(clock),
            nickName = nickName,
            // Transfer
            transferLimit = transferLimit,
            totalBytes = totalBytes,
            // Bandwidth
            bandwidthLimit = bandwidthLimit,
            limiter = BandwidthLimiter(),
        )
      }
    }

    @CheckResult
    fun create(
        hostNameOrIp: String,
        clock: Clock,
        nickName: String = "",
        transferLimit: TransferAmount? = null,
        bandwidthLimit: TransferAmount? = null,
    ): TetherClient {
      return create(
          hostNameOrIp = hostNameOrIp,
          clock = clock,
          nickName = nickName,
          bandwidthLimit = bandwidthLimit,
          transferLimit = transferLimit,
          totalBytes = ByteTransferReport.EMPTY,
      )
    }

    @TestOnly
    @CheckResult
    @VisibleForTesting
    fun testCreate(
        hostNameOrIp: String,
        clock: Clock,
        nickName: String,
        transferLimit: TransferAmount? = null,
        bandwidthLimit: TransferAmount? = null,
        totalBytes: ByteTransferReport,
    ): TetherClient {
      return create(
          hostNameOrIp = hostNameOrIp,
          clock = clock,
          nickName = nickName,
          transferLimit = transferLimit,
          bandwidthLimit = bandwidthLimit,
          totalBytes = totalBytes,
      )
    }
  }
}

@CheckResult
fun TetherClient.key(): String {
  return when (this) {
    is HostNameClient -> this.hostname
    is IpAddressClient -> this.ip
  }
}
