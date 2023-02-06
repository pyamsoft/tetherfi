package com.pyamsoft.tetherfi.server.widi

import androidx.annotation.CheckResult
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.pyamsoft.tetherfi.server.Server
import com.pyamsoft.tetherfi.server.status.RunningStatus

interface WiDiNetworkStatus : Server {

  fun updateNetworkInfo()

  suspend fun onGroupInfoChanged(onChange: (GroupInfo) -> Unit)

  suspend fun onConnectionInfoChanged(onChange: (ConnectionInfo) -> Unit)

  suspend fun onProxyStatusChanged(block: suspend (RunningStatus) -> Unit)

  @Stable
  @Immutable
  sealed class GroupInfo {

    data class Connected
    internal constructor(
        val ssid: String,
        val password: String,
    ) : GroupInfo()

    object Empty : GroupInfo()

    data class Error internal constructor(val error: Throwable) : GroupInfo()

    @CheckResult
    fun update(onUpdate: (Connected) -> Connected): GroupInfo {
      return when (this) {
        is Connected -> onUpdate(this)
        is Empty -> this
        is Error -> this
      }
    }
  }

  @Stable
  @Immutable
  sealed class ConnectionInfo {
    data class Connected
    internal constructor(
        val ip: String,
        val hostName: String,
    ) : ConnectionInfo()

    object Empty : ConnectionInfo()

    data class Error internal constructor(val error: Throwable) : ConnectionInfo()

    @CheckResult
    fun update(onUpdate: (Connected) -> Connected): ConnectionInfo {
      return when (this) {
        is Connected -> onUpdate(this)
        is Empty -> this
        is Error -> this
      }
    }
  }
}
