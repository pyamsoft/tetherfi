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
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.pyamsoft.tetherfi.server.IP_ADDRESS_REGEX
import java.time.Clock
import java.time.LocalDateTime

private val UNIT_JUMP = 1024UL

@Stable
@Immutable
sealed class TetherClient(
    open val nickName: String,
    open val mostRecentlySeen: LocalDateTime,
    open val limit: BandwidthLimit?,
    protected open val totalBytes: ByteTransferReport,
) {

  val transferToInternet by lazy { parseBandwidth(totalBytes.proxyToInternet) }
  val transferFromInternet by lazy { parseBandwidth(totalBytes.internetToProxy) }

  @CheckResult
  private fun parseBandwidth(total: ULong): BandwidthLimit {
    var amount = total
    var suffix = BandwidthUnit.BYTE
    while (amount > UNIT_JUMP) {
      suffix = mapSuffixToNextLargest(amount, suffix)
      amount /= UNIT_JUMP
    }

    return BandwidthLimit(
        amount = amount,
        unit = suffix,
    )
  }

  @CheckResult
  private fun mapSuffixToNextLargest(amount: ULong, suffix: BandwidthUnit): BandwidthUnit =
      when (suffix) {
        BandwidthUnit.BYTE -> BandwidthUnit.KB
        BandwidthUnit.KB -> BandwidthUnit.MB
        BandwidthUnit.MB -> BandwidthUnit.GB
        BandwidthUnit.GB -> BandwidthUnit.TB
        BandwidthUnit.TB -> BandwidthUnit.PB
        else -> throw IllegalStateException("Bytes payload too big: $amount$suffix")
      }

  @CheckResult
  private fun BandwidthLimit.isOver(limit: BandwidthLimit): Boolean {
    // If unit is larger than other unit, we are over
    if (this.unit > limit.unit) {
      return false
    }

    // If units are the same, count the amount
    if (this.unit == limit.unit) {
      return this.amount > limit.amount
    }

    // Otherwise, I guess we are good
    return false
  }

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
        if (IP_ADDRESS_REGEX.matches(hostNameOrIp)) {
          return ip == hostNameOrIp
        }

        return false
      }
      is HostNameClient -> {
        if (!IP_ADDRESS_REGEX.matches(hostNameOrIp)) {
          return hostname == hostNameOrIp
        }

        return false
      }
    }
  }

  @CheckResult
  fun isOverBandwidthLimit(): Boolean {
    // No limit, we are fine
    val l = limit ?: return false

    // Our transfer unit is larger than our limit
    if (transferToInternet.isOver(l)) {
      return true
    }

    if (transferFromInternet.isOver(l)) {
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
    fun create(hostNameOrIp: String, clock: Clock): TetherClient {
      return if (IP_ADDRESS_REGEX.matches(hostNameOrIp)) {
        IpAddressClient(
            ip = hostNameOrIp,
            mostRecentlySeen = LocalDateTime.now(clock),
            nickName = "",
            limit = null,
            totalBytes = ByteTransferReport.EMPTY,
        )
      } else {
        HostNameClient(
            hostname = hostNameOrIp,
            mostRecentlySeen = LocalDateTime.now(clock),
            nickName = "",
            limit = null,
            totalBytes = ByteTransferReport.EMPTY,
        )
      }
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
