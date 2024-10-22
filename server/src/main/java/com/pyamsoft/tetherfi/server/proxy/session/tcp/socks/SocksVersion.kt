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

package com.pyamsoft.tetherfi.server.proxy.session.tcp.socks

import androidx.annotation.CheckResult

internal enum class SOCKSVersion(
    val version: Byte,
    val replyVersion: Byte,
    val successCode: Byte,
    val unreachableHostCode: Byte,
    val connectionRefusedCode: Byte
) {
    SOCKS4(
        version = 4,
        replyVersion = 0,
        successCode = 90,
        unreachableHostCode = 91,
        connectionRefusedCode = 91
    ),
    SOCKS5(
        version = 5,
        replyVersion = 5,
        successCode = 0,
        unreachableHostCode = 4,
        connectionRefusedCode = 5
    );

    companion object {

        @JvmStatic
        @CheckResult
        fun fromVersion(version: Byte): SOCKSVersion = when (version) {
            SOCKS4.version -> SOCKS4
            SOCKS5.version -> SOCKS5
            else -> throw SOCKSException("Invalid SOCKS version: $version")
        }
    }
}