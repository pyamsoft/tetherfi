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

package com.pyamsoft.tetherfi.server.event

import androidx.annotation.CheckResult

@ConsistentCopyVisibility
internal data class ProxyRequest
internal constructor(
    override val method: String,
    val host: String,
    val port: Int,
    val version: String,
    val raw: String,
    val file: String,
) : TunnelRequest(method) {

  val httpRequest by lazy {
    // Strip off the hostname just leaving file name for requests
    // NOTE(Peter): We used to do this and things would work because most network requests
    // would also send over a HOST header. But it looks like also just sending the unchanged
    // first line of the request also works just fine. So I do not know why we did this parsing
    // actually.
    "$method $file $version"
  }
}

internal abstract class TunnelRequest(
    open val method: String,
) {

  @CheckResult
  fun isHttpsConnectRequest(): Boolean {
    return method == "CONNECT"
  }
}
