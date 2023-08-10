package com.pyamsoft.tetherfi.status.vpn

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.core.content.getSystemService
import com.pyamsoft.pydroid.core.requireNotNull
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AndroidVpnChecker
@Inject
internal constructor(
    private val context: Context,
) : VpnChecker {

  private val manager by lazy {
    context.applicationContext.getSystemService<ConnectivityManager>().requireNotNull()
  }

  override fun isUsingVpn(): Boolean {
    val network = manager.activeNetwork
    val capabilities = manager.getNetworkCapabilities(network)

    if (capabilities == null) {
      Timber.w("Could not retrieve NetworkCapabilities")
      return false
    }

    return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
  }
}
