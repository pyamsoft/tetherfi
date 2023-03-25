/*
 * Copyright 2023 pyamsoft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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
import java.time.LocalDateTime

sealed class TetherClient(
    open val firstSeen: LocalDateTime,
) {
  data class IpAddress(
      val ip: String,
      override val firstSeen: LocalDateTime,
  ) :
      TetherClient(
          firstSeen = firstSeen,
      )
  data class HostName(
      val hostname: String,
      override val firstSeen: LocalDateTime,
  ) :
      TetherClient(
          firstSeen = firstSeen,
      )
}

@CheckResult
fun TetherClient.key(): String {
    return when (this) {
        is TetherClient.HostName -> this.hostname
        is TetherClient.IpAddress -> this.ip
    }
}
