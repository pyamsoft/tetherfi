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

package com.pyamsoft.tetherfi.server.broadcast.wifidirect

import android.net.wifi.p2p.WifiP2pManager
import androidx.annotation.CheckResult
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.pyamsoft.tetherfi.server.status.RunningStatus

@Stable
@Immutable
data class WiFiDirectError(
    val reason: Reason,
    override val throwable: Throwable,
) : RunningStatus.Error(throwable) {

  @Stable
  @Immutable
  sealed class Reason(
      open val code: Int,
      val displayReason: String,
  ) {

    data object P2PUnsupported : Reason(WifiP2pManager.P2P_UNSUPPORTED, "P2P Unsupported")

    data object NoServiceRequests :
        Reason(WifiP2pManager.NO_SERVICE_REQUESTS, "No Service Requests")

    data object Error : Reason(WifiP2pManager.ERROR, "Error")

    data object Busy : Reason(WifiP2pManager.BUSY, "Busy")

    @ConsistentCopyVisibility
    data class Unknown internal constructor(override val code: Int) : Reason(code, "Unknown")

    companion object {

      @JvmStatic
      @CheckResult
      internal fun parseReason(reason: Int): Reason {
        return when (reason) {
          WifiP2pManager.P2P_UNSUPPORTED -> P2PUnsupported
          WifiP2pManager.NO_SERVICE_REQUESTS -> NoServiceRequests
          WifiP2pManager.ERROR -> Error
          WifiP2pManager.BUSY -> Busy
          else -> Unknown(reason)
        }
      }
    }
  }
}
