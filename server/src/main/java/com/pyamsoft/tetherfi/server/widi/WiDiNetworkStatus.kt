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

package com.pyamsoft.tetherfi.server.widi

import androidx.annotation.CheckResult
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.pyamsoft.tetherfi.server.Server
import com.pyamsoft.tetherfi.server.status.RunningStatus
import kotlinx.coroutines.flow.Flow

interface WiDiNetworkStatus : Server {

  suspend fun updateNetworkInfo()

  @CheckResult fun onGroupInfoChanged(): Flow<GroupInfo>

  @CheckResult fun onConnectionInfoChanged(): Flow<ConnectionInfo>

  @CheckResult fun onProxyStatusChanged(): Flow<RunningStatus>

  @Stable
  @Immutable
  sealed interface GroupInfo {

    object Unchanged : GroupInfo

    data class Connected
    internal constructor(
        val ssid: String,
        val password: String,
    ) : GroupInfo

    object Empty : GroupInfo

    data class Error
    internal constructor(
        val error: Throwable,
    ) : GroupInfo

    @CheckResult
    fun update(onUpdate: (Connected) -> Connected): GroupInfo {
      return when (this) {
        is Connected -> onUpdate(this)
        is Empty -> this
        is Error -> this
        is Unchanged -> this
      }
    }
  }

  @Stable
  @Immutable
  sealed interface ConnectionInfo {
    object Unchanged : ConnectionInfo

    data class Connected
    internal constructor(
        val hostName: String,
    ) : ConnectionInfo

    object Empty : ConnectionInfo

    data class Error
    internal constructor(
        val error: Throwable,
    ) : ConnectionInfo

    @CheckResult
    fun update(onUpdate: (Connected) -> Connected): ConnectionInfo {
      return when (this) {
        is Connected -> onUpdate(this)
        is Empty -> this
        is Error -> this
        is Unchanged -> this
      }
    }
  }
}
