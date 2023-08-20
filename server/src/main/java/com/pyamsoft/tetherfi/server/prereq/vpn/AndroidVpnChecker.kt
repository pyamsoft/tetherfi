package com.pyamsoft.tetherfi.server.prereq.vpn

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.core.content.getSystemService
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.tetherfi.server.ServerPreferences
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber

@Singleton
internal class AndroidVpnChecker
@Inject
internal constructor(
    private val context: Context,
    private val preferences: ServerPreferences,
) : VpnChecker {

  private val manager by lazy {
    context.applicationContext.getSystemService<ConnectivityManager>().requireNotNull()
  }

  override suspend fun isUsingVpn(): Boolean =
      withContext(context = Dispatchers.Default) {
        if (preferences.listenForStartIgnoreVpn().first()) {
          Timber.w("Ignore VPN start blocker")
          return@withContext false
        }

        val network = manager.activeNetwork
        val capabilities = manager.getNetworkCapabilities(network)

        if (capabilities == null) {
          Timber.w("Could not retrieve NetworkCapabilities")
          return@withContext false
        }

        return@withContext capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
      }
}
