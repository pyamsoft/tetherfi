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

import androidx.annotation.CheckResult
import com.pyamsoft.tetherfi.server.broadcast.BroadcastNetworkStatus
import java.util.UUID

private val RNDIS_SSID by lazy { "rndis-ssid-${UUID.randomUUID()}" }
private val RNDIS_PASSWD by lazy { "rndis-passwd-${UUID.randomUUID()}" }

private val RNDIS by lazy {
    BroadcastNetworkStatus.GroupInfo.Connected(
        ssid = RNDIS_SSID,
        password = RNDIS_PASSWD,
    )
}


@CheckResult
fun BroadcastNetworkStatus.GroupInfo.Connected.isRNDISNetwork(): Boolean {
    return this.ssid == RNDIS_SSID && this.password == RNDIS_PASSWD
}

@CheckResult
fun useRNDISGroupInfo(): BroadcastNetworkStatus.GroupInfo {
    return RNDIS
}
