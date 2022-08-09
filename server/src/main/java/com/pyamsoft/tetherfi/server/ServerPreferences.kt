package com.pyamsoft.tetherfi.server

import androidx.annotation.CheckResult
import kotlinx.coroutines.flow.Flow

interface ServerPreferences {

  @CheckResult suspend fun listenForSsidChanges(): Flow<String>

  suspend fun setSsid(ssid: String)

  @CheckResult suspend fun listenForPasswordChanges(): Flow<String>

  suspend fun setPassword(password: String)

  @CheckResult suspend fun listenForPortChanges(): Flow<Int>

  suspend fun setPort(port: Int)

  @CheckResult suspend fun listenForNetworkBandChanges(): Flow<ServerNetworkBand>

  suspend fun setNetworkBand(band: ServerNetworkBand)
}
