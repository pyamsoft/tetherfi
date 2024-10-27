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

package com.pyamsoft.tetherfi.server.proxy.session.tcp.socks.five

import androidx.annotation.CheckResult
import com.pyamsoft.tetherfi.server.proxy.session.tcp.socks.BaseSOCKSImplementation

internal enum class SOCKS5AddressType(internal val byte: Byte) :
    BaseSOCKSImplementation.SOCKSAddressType {
  IPV4(1),
  DOMAIN_NAME(3),
  IPV6(4);

  companion object {

    @JvmStatic
    @CheckResult
    fun fromAddressType(addressType: Byte): SOCKS5AddressType? =
        when (addressType) {
          IPV4.byte -> IPV4
          DOMAIN_NAME.byte -> DOMAIN_NAME
          IPV6.byte -> DOMAIN_NAME
          else -> null
        }
  }
}
