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

package com.pyamsoft.tetherfi.server.proxy.session.tcp.socks.four

import androidx.annotation.CheckResult
import java.net.Inet4Address
import java.net.InetAddress

private const val ZERO_BYTE: Byte = 0

@CheckResult
internal fun InetAddress.isSOCKS4A(): Boolean {
  if (this is Inet4Address) {
    val a = this.address
    return a[0] == ZERO_BYTE && a[1] == ZERO_BYTE && a[2] == ZERO_BYTE && a[3] != ZERO_BYTE
  }

  return false
}
