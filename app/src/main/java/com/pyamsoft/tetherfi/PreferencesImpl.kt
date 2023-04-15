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

package com.pyamsoft.tetherfi

import android.content.Context
import androidx.annotation.CheckResult
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.pyamsoft.pydroid.core.ThreadEnforcer
import com.pyamsoft.pydroid.util.booleanFlow
import com.pyamsoft.pydroid.util.intFlow
import com.pyamsoft.pydroid.util.stringFlow
import com.pyamsoft.tetherfi.server.ServerDefaults
import com.pyamsoft.tetherfi.server.ServerNetworkBand
import com.pyamsoft.tetherfi.server.ServerPreferences
import com.pyamsoft.tetherfi.service.ServicePreferences
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Singleton
internal class PreferencesImpl
@Inject
internal constructor(
    enforcer: ThreadEnforcer,
    context: Context,
) : ServerPreferences, ServicePreferences {

  private val preferences by lazy {
    enforcer.assertOffMainThread()
    PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
  }

  private val fallbackPassword by lazy(LazyThreadSafetyMode.NONE) { generateRandomPassword() }

  override suspend fun listenForWakeLockChanges(): Flow<Boolean> =
      withContext(context = Dispatchers.IO) { preferences.booleanFlow(WAKE_LOCK, true) }

  override suspend fun setWakeLock(keep: Boolean) =
      withContext(context = Dispatchers.IO) { preferences.edit { putBoolean(WAKE_LOCK, keep) } }

  override suspend fun listenForWiFiLockChanges(): Flow<Boolean> =
      withContext(context = Dispatchers.IO) { preferences.booleanFlow(WIFI_LOCK, true) }

  override suspend fun setWiFiLock(keep: Boolean) =
      withContext(context = Dispatchers.IO) { preferences.edit { putBoolean(WIFI_LOCK, keep) } }

  override suspend fun listenForSsidChanges(): Flow<String> =
      withContext(context = Dispatchers.IO) { preferences.stringFlow(SSID, ServerDefaults.SSID) }

  override suspend fun setSsid(ssid: String) =
      withContext(context = Dispatchers.IO) { preferences.edit { putString(SSID, ssid) } }

  override suspend fun listenForPasswordChanges(): Flow<String> =
      withContext(context = Dispatchers.IO) {
        var pass = preferences.getString(PASSWORD, "")

        // Ensure a random password is generated
        if (pass.isNullOrBlank()) {
          pass = fallbackPassword
          setPassword(pass)
        }

        return@withContext preferences.stringFlow(PASSWORD, pass)
      }

  override suspend fun setPassword(password: String) =
      withContext(context = Dispatchers.IO) { preferences.edit { putString(PASSWORD, password) } }

  override suspend fun listenForPortChanges(): Flow<Int> =
      withContext(context = Dispatchers.IO) { preferences.intFlow(PORT, ServerDefaults.PORT) }

  override suspend fun setPort(port: Int) =
      withContext(context = Dispatchers.IO) { preferences.edit { putInt(PORT, port) } }

  override suspend fun listenForNetworkBandChanges(): Flow<ServerNetworkBand> =
      withContext(context = Dispatchers.IO) {
        val fallback = ServerDefaults.NETWORK_BAND.name
        return@withContext preferences.stringFlow(NETWORK_BAND, fallback).map {
          ServerNetworkBand.valueOf(it)
        }
      }

  override suspend fun setNetworkBand(band: ServerNetworkBand) =
      withContext(context = Dispatchers.IO) {
        preferences.edit { putString(NETWORK_BAND, band.name) }
      }

  companion object {

    private const val SSID = "key_ssid_1"
    private const val PASSWORD = "key_password_1"
    private const val PORT = "key_port_1"
    private const val NETWORK_BAND = "key_network_band_1"

    private const val WAKE_LOCK = "key_wake_lock_1"
    private const val WIFI_LOCK = "key_wifi_lock_1"

    private const val ALL_CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"

    @CheckResult
    private fun generateRandomPassword(size: Int = 8): String {
      val chars = ALL_CHARS

      var pass = ""
      while (true) {
        pass += chars[Random.nextInt(0, chars.length)]

        // Stop once generated
        if (pass.length >= size) {
          break
        }
      }
      return pass
    }
  }
}
