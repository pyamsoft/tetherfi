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
import com.pyamsoft.tetherfi.server.proxy.session.tcp.ProxyRequest

internal sealed interface SOCKSVersion : ProxyRequest {

    data object SOCKS4 : SOCKSVersion {
        override val valid: Boolean = true
    }

    data object SOCKS5 : SOCKSVersion {
        override val valid: Boolean = true
    }

    data object Invalid : SOCKSVersion {
        override val valid: Boolean = false
    }

    companion object {

        private const val VERSION_4: Byte = 4
        private const val VERSION_5: Byte = 5

        @JvmStatic
        @CheckResult
        fun fromVersion(version: Byte): SOCKSVersion = when (version) {
            VERSION_4 -> SOCKS4
            VERSION_5 -> SOCKS5
            else -> Invalid
        }
    }
}