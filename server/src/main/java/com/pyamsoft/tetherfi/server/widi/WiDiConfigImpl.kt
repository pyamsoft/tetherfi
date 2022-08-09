package com.pyamsoft.tetherfi.server.widi

import android.net.wifi.p2p.WifiP2pConfig
import android.os.Build
import androidx.annotation.CheckResult
import androidx.annotation.RequiresApi
import com.pyamsoft.tetherfi.server.ServerDefaults
import com.pyamsoft.tetherfi.server.ServerNetworkBand
import com.pyamsoft.tetherfi.server.ServerPreferences
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
internal class WiDiConfigImpl
@Inject
internal constructor(
    private val preferences: ServerPreferences,
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

  override suspend fun getConfiguration(): WifiP2pConfig? {
    return if (ServerDefaults.canUseCustomConfig()) {
      WifiP2pConfig.Builder()
          .setNetworkName(ServerDefaults.asSsid(getPreferredSsid()))
          .setPassphrase(getPreferredPassword())
          .setGroupOperatingBand(getPreferredBand())
          .build()
    } else {
      null
    }
  }
}
