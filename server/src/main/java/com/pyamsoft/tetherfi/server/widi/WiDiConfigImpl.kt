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

import android.net.wifi.p2p.WifiP2pConfig
import android.os.Build
import androidx.annotation.CheckResult
import androidx.annotation.RequiresApi
import com.pyamsoft.tetherfi.server.ConfigPreferences
import com.pyamsoft.tetherfi.server.ServerDefaults
import com.pyamsoft.tetherfi.server.ServerNetworkBand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class WiDiConfigImpl
@Inject
internal constructor(
    private val preferences: ConfigPreferences,
) : WiDiConfig {

  @CheckResult
  @RequiresApi(Build.VERSION_CODES.Q)
  private suspend fun getPreferredSsid(): String {
    return preferences.listenForSsidChanges().first()
  }

  @CheckResult
  @RequiresApi(Build.VERSION_CODES.Q)
  private suspend fun getPreferredPassword(): String {
    return preferences.listenForPasswordChanges().first()
  }

  @CheckResult
  @RequiresApi(Build.VERSION_CODES.Q)
  private suspend fun getPreferredBand(): Int {
    return when (preferences.listenForNetworkBandChanges().first()) {
      ServerNetworkBand.MODERN -> WifiP2pConfig.GROUP_OWNER_BAND_5GHZ
      ServerNetworkBand.LEGACY -> WifiP2pConfig.GROUP_OWNER_BAND_2GHZ
    }
  }

  override suspend fun getConfiguration(): WifiP2pConfig? =
      withContext(context = Dispatchers.Default) {
        if (!ServerDefaults.canUseCustomConfig()) {
          return@withContext null
        }

        val ssid = ServerDefaults.asSsid(getPreferredSsid())
        val passwd = getPreferredPassword()
        val band = getPreferredBand()

        return@withContext WifiP2pConfig.Builder()
            .setNetworkName(ssid)
            .setPassphrase(passwd)
            .setGroupOperatingBand(band)
            .build()
      }
}
