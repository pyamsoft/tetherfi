package com.pyamsoft.widefi

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.pyamsoft.pydroid.core.Enforcer
import com.pyamsoft.widefi.server.ServerDefaults
import com.pyamsoft.widefi.server.ServerPreferences
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
internal class PreferencesImpl @Inject internal constructor(context: Context) : ServerPreferences {

  private val preferences by lazy {
    PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
  }

  override suspend fun getSsid(): String =
      withContext(context = Dispatchers.IO) {
        Enforcer.assertOffMainThread()

        val fallback = ServerDefaults.SSID
        return@withContext preferences.getString(SSID, fallback).orEmpty().ifBlank { fallback }
      }

  override suspend fun setSsid(ssid: String) =
      withContext(context = Dispatchers.IO) {
        Enforcer.assertOffMainThread()

        preferences.edit { putString(SSID, ssid) }
      }

  override suspend fun getPassword(): String =
      withContext(context = Dispatchers.IO) {
        Enforcer.assertOffMainThread()

        val fallback = ServerDefaults.PASSWORD
        return@withContext preferences.getString(PASSWORD, fallback).orEmpty().ifBlank { fallback }
      }

  override suspend fun setPassword(password: String) =
      withContext(context = Dispatchers.IO) {
        Enforcer.assertOffMainThread()

        preferences.edit { putString(PASSWORD, password) }
      }

  override suspend fun getPort(): Int =
      withContext(context = Dispatchers.IO) {
        Enforcer.assertOffMainThread()

        val port = preferences.getInt(PORT, ServerDefaults.PORT)
        return@withContext if (port <= 0) ServerDefaults.PORT else port
      }

  override suspend fun setPort(port: Int) =
      withContext(context = Dispatchers.IO) {
        Enforcer.assertOffMainThread()

        preferences.edit { putInt(PORT, port) }
      }

  override suspend fun getNetworkBand(): ServerPreferences.NetworkBand =
      withContext(context = Dispatchers.IO) {
        Enforcer.assertOffMainThread()

        val fallback = ServerDefaults.NETWORK_BAND.name
        val band = preferences.getString(NETWORK_BAND, fallback).orEmpty().ifBlank { fallback }
        return@withContext ServerPreferences.NetworkBand.valueOf(band)
      }

  override suspend fun setNetworkBand(band: ServerPreferences.NetworkBand) =
      withContext(context = Dispatchers.IO) {
        Enforcer.assertOffMainThread()

        preferences.edit { putString(NETWORK_BAND, band.name) }
      }

  companion object {

    private const val SSID = "key_ssid_1"
    private const val PASSWORD = "key_password_1"
    private const val PORT = "key_port_1"
    private const val NETWORK_BAND = "key_network_band_1"
  }
}
