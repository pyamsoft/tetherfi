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

enum class BandwidthUnit(val displayName: String) {
    BYTE("bytes"),
    KB("KB"),
    MB("MB"),
    GB("DB"),
    TB("TB"),
    PB("PB"),
}

@Stable
@Immutable
data class BandwidthLimit(
    val amount: ULong,
    val unit: BandwidthUnit,
) {

    val display by lazy {
        "$amount ${unit.displayName}"
    }
}

@Stable
@Immutable
sealed class TetherClient(
    open val nickName: String,
    open val mostRecentlySeen: LocalDateTime,
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
    fun matches(o: TetherClient): Boolean {
        when (this) {
            is IpAddress -> {
                if (o is IpAddress) {
                    return ip == o.ip
                }

                return false
            }

            is HostName -> {
                if (o is HostName) {
                    return hostname == o.hostname
                }

                return false
            }
        }
    }

    @CheckResult
    fun matches(hostNameOrIp: String): Boolean {
        when (this) {
            is IpAddress -> {
                if (IP_ADDRESS_REGEX.matches(hostNameOrIp)) {
                    return ip == hostNameOrIp
                }

                return false
            }

            is HostName -> {
                if (!IP_ADDRESS_REGEX.matches(hostNameOrIp)) {
                    return hostname == hostNameOrIp
                }

                return false
            }
        }
    }

    @CheckResult
    fun mergeReport(report: ByteTransferReport): ByteTransferReport {
        return report.copy(
            internetToProxy = report.internetToProxy + totalBytes.internetToProxy,
            proxyToInternet = report.proxyToInternet + totalBytes.proxyToInternet,
        )
    }

    data class IpAddress
    internal constructor(
        val ip: String,
        override val nickName: String,
        override val mostRecentlySeen: LocalDateTime,
        override val totalBytes: ByteTransferReport,
    ) :
        TetherClient(
            nickName = nickName,
            mostRecentlySeen = mostRecentlySeen,
            totalBytes = totalBytes,
        )

    data class HostName
    internal constructor(
        val hostname: String,
        override val nickName: String,
        override val mostRecentlySeen: LocalDateTime,
        override val totalBytes: ByteTransferReport,
    ) :
        TetherClient(
            nickName = nickName,
            mostRecentlySeen = mostRecentlySeen,
            totalBytes = totalBytes,
        )

    companion object {

        @CheckResult
        fun create(hostNameOrIp: String, clock: Clock): TetherClient {
            return if (IP_ADDRESS_REGEX.matches(hostNameOrIp)) {
                IpAddress(
                    ip = hostNameOrIp,
                    mostRecentlySeen = LocalDateTime.now(clock),
                    nickName = "",
                    totalBytes = ByteTransferReport.EMPTY,
                )
            } else {
                HostName(
                    hostname = hostNameOrIp,
                    mostRecentlySeen = LocalDateTime.now(clock),
                    nickName = "",
                    totalBytes = ByteTransferReport.EMPTY,
                )
            }
        }
    }
}

@CheckResult
fun TetherClient.key(): String {
    return when (this) {
        is TetherClient.HostName -> this.hostname
        is TetherClient.IpAddress -> this.ip
    }
}
