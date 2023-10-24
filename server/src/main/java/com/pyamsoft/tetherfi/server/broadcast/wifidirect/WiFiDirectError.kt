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
