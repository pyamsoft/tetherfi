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
import android.content.SharedPreferences
import androidx.annotation.CheckResult
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.pyamsoft.pydroid.core.ThreadEnforcer
import com.pyamsoft.pydroid.util.preferenceBooleanFlow
import com.pyamsoft.pydroid.util.preferenceIntFlow
import com.pyamsoft.pydroid.util.preferenceStringFlow
import com.pyamsoft.tetherfi.core.InAppRatingPreferences
import com.pyamsoft.tetherfi.core.Timber
import com.pyamsoft.tetherfi.server.ConfigPreferences
import com.pyamsoft.tetherfi.server.ServerDefaults
import com.pyamsoft.tetherfi.server.ServerNetworkBand
import com.pyamsoft.tetherfi.server.ServerPerformanceLimit
import com.pyamsoft.tetherfi.server.ServerPreferences
import com.pyamsoft.tetherfi.service.ServicePreferences
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Singleton
internal class PreferencesImpl
@Inject
internal constructor(
    private val enforcer: ThreadEnforcer,
    context: Context,
) : ServerPreferences, ServicePreferences, InAppRatingPreferences, ConfigPreferences {

  private val preferences by lazy {
    enforcer.assertOffMainThread()
    PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
  }

  // Keep this lazy so that the fallback password is always the same
  private val fallbackPassword by lazy { PasswordGenerator.generate() }

  private val scope by lazy {
    CoroutineScope(
        context = SupervisorJob() + Dispatchers.Default + CoroutineName(this::class.java.name),
    )
  }

  private fun setPreference(block: SharedPreferences.Editor.() -> Unit) {
    scope.launch {
      enforcer.assertOffMainThread()
      preferences.edit { block() }
    }
  }

  private fun updatePreference(block: SharedPreferences.() -> Unit) {
    scope.launch {
      enforcer.assertOffMainThread()
      preferences.block()
    }
  }

  @CheckResult
  private fun isInAppRatingAlreadyShown(): Boolean {
    enforcer.assertOffMainThread()
    val version = preferences.getInt(IN_APP_RATING_SHOWN_VERSION, 0)
    return version.isInAppRatingAlreadyShown()
  }

  @CheckResult
  private fun Int.isInAppRatingAlreadyShown(): Boolean {
    enforcer.assertOffMainThread()
    return this > 0 && this == BuildConfig.VERSION_CODE
  }

  private fun SharedPreferences.updatePassword(
      password: String,
      commit: Boolean = false,
  ) {
    this.edit(commit = commit) { putString(PASSWORD, password) }
  }

  override fun listenForWakeLockChanges(): Flow<Boolean> =
      preferenceBooleanFlow(WAKE_LOCK, true) { preferences }.flowOn(context = Dispatchers.Default)

  override fun setWakeLock(keep: Boolean) = setPreference { putBoolean(WAKE_LOCK, keep) }

  override fun listenForWiFiLockChanges(): Flow<Boolean> =
      preferenceBooleanFlow(WIFI_LOCK, true) { preferences }.flowOn(context = Dispatchers.Default)

  override fun setWiFiLock(keep: Boolean) = setPreference { putBoolean(WIFI_LOCK, keep) }

  override fun listenForSsidChanges(): Flow<String> =
      preferenceStringFlow(SSID, ServerDefaults.SSID) { preferences }
          .flowOn(context = Dispatchers.Default)

  override fun setSsid(ssid: String) = setPreference { putString(SSID, ssid) }

  override fun listenForPasswordChanges(): Flow<String> =
      preferenceStringFlow(PASSWORD, fallbackPassword) {
            preferences.also { p ->
              if (!p.contains(PASSWORD)) {
                // Commit this edit so that it fires immediately before we process again
                p.updatePassword(fallbackPassword, commit = true)
              }
            }
          }
          .flowOn(context = Dispatchers.Default)

  override fun setPassword(password: String) = setPreference {
    preferences.updatePassword(password)
  }

  override fun listenForPortChanges(): Flow<Int> =
      preferenceIntFlow(PORT, ServerDefaults.PORT) { preferences }
          .flowOn(context = Dispatchers.Default)

  override fun setPort(port: Int) = setPreference { putInt(PORT, port) }

  override fun listenForNetworkBandChanges(): Flow<ServerNetworkBand> =
      preferenceStringFlow(NETWORK_BAND, ServerDefaults.NETWORK_BAND.name) { preferences }
          .map { ServerNetworkBand.valueOf(it) }
          .flowOn(context = Dispatchers.Default)

  override fun setNetworkBand(band: ServerNetworkBand) = setPreference {
    putString(NETWORK_BAND, band.name)
  }

  override fun listenForStartIgnoreVpn(): Flow<Boolean> =
      preferenceBooleanFlow(START_IGNORE_VPN, false) { preferences }
          .flowOn(context = Dispatchers.Default)

  override fun setStartIgnoreVpn(ignore: Boolean) = setPreference {
    putBoolean(START_IGNORE_VPN, ignore)
  }

  override fun listenForShutdownWithNoClients(): Flow<Boolean> =
      preferenceBooleanFlow(SHUTDOWN_NO_CLIENTS, false) { preferences }
          .flowOn(context = Dispatchers.Default)

  override fun setShutdownWithNoClients(shutdown: Boolean) = setPreference {
    putBoolean(SHUTDOWN_NO_CLIENTS, shutdown)
  }

  override fun listenForProxyBindAll(): Flow<Boolean> =
      preferenceBooleanFlow(PROXY_BIND_ALL, false) { preferences }
          .flowOn(context = Dispatchers.Default)

  override fun setProxyBindAll(bind: Boolean) = setPreference { putBoolean(PROXY_BIND_ALL, bind) }

  override fun listenShowInAppRating(): Flow<Boolean> =
      combineTransform(
              preferenceIntFlow(IN_APP_HOTSPOT_USED, 0) { preferences },
              preferenceIntFlow(IN_APP_DEVICES_CONNECTED, 0) { preferences },
              preferenceIntFlow(IN_APP_APP_OPENED, 0) { preferences },
              preferenceIntFlow(IN_APP_RATING_SHOWN_VERSION, 0) { preferences },
          ) { hotspotUsed, devicesConnected, appOpened, lastVersionShown ->
            enforcer.assertOffMainThread()

            Timber.d {
              "In app rating check: ${
                mapOf(
                    "lastVersion" to lastVersionShown,
                    "isAlreadyShown" to lastVersionShown.isInAppRatingAlreadyShown(),
                    "hotspotUsed" to hotspotUsed,
                    "devicesConnected" to devicesConnected,
                    "appOpened" to appOpened,
                )
            }"
            }

            if (lastVersionShown.isInAppRatingAlreadyShown()) {
              Timber.w { "Already shown in-app rating for version: $lastVersionShown" }
              emit(false)
            } else {
              val show = hotspotUsed >= 3 && devicesConnected >= 2 && appOpened >= 7
              emit(show)

              if (show) {
                // Commit this edit so that it fires immediately before we process again
                preferences.edit(commit = true) {
                  // Reset the previous flags
                  putInt(IN_APP_APP_OPENED, 0)
                  putInt(IN_APP_HOTSPOT_USED, 0)
                  putInt(IN_APP_DEVICES_CONNECTED, 0)

                  // And mark the latest version
                  putInt(IN_APP_RATING_SHOWN_VERSION, BuildConfig.VERSION_CODE)
                }
              }
            }
          }
          // Need this or we run on the main thread
          .flowOn(context = Dispatchers.Default)

  override fun markHotspotUsed() = updatePreference {
    if (!isInAppRatingAlreadyShown()) {
      // Not atomic because shared prefs are lame
      updateInt(IN_APP_HOTSPOT_USED, 0) { it + 1 }
    }
  }

  override fun markAppOpened() = updatePreference {
    if (!isInAppRatingAlreadyShown()) {
      // Not atomic because shared prefs are lame
      updateInt(IN_APP_APP_OPENED, 0) { it + 1 }
    }
  }

  override fun markDeviceConnected() = updatePreference {
    if (!isInAppRatingAlreadyShown()) {
      // Not atomic because shared prefs are lame
      updateInt(IN_APP_DEVICES_CONNECTED, 0) { it + 1 }
    }
  }

  override fun listenForPerformanceLimits(): Flow<ServerPerformanceLimit> =
      preferenceIntFlow(
              SERVER_LIMITS,
              ServerPerformanceLimit.Defaults.BOUND_3N_CPU.coroutineLimit,
          ) {
            preferences
          }
          .map { ServerPerformanceLimit.create(it) }

  override fun setServerPerformanceLimit(limit: ServerPerformanceLimit) = setPreference {
    putInt(SERVER_LIMITS, limit.coroutineLimit)
  }

  private fun SharedPreferences.updateInt(key: String, defaultValue: Int, update: (Int) -> Int) {
    val self = this

    // Kinda atomic-ey
    while (true) {
      val prevValue = self.getInt(key, defaultValue)
      val nextValue = update(prevValue)
      synchronized(self) { self.edit { putInt(key, nextValue) } }
      return
    }
  }

  private object PasswordGenerator {

    private const val ALL_CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"

    @JvmStatic
    @CheckResult
    fun generate(size: Int = 8): String {
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

  companion object {

    private const val SSID = "key_ssid_1"
    private const val PASSWORD = "key_password_1"
    private const val PORT = "key_port_1"
    private const val NETWORK_BAND = "key_network_band_1"

    private const val WAKE_LOCK = "key_wake_lock_1"
    private const val WIFI_LOCK = "key_wifi_lock_1"

    private const val IN_APP_HOTSPOT_USED = "key_in_app_hotspot_used_1"
    private const val IN_APP_DEVICES_CONNECTED = "key_in_app_devices_connected_1"
    private const val IN_APP_APP_OPENED = "key_in_app_app_opened_1"

    private const val IN_APP_RATING_SHOWN_VERSION = "key_in_app_rating_shown_version"

    private const val START_IGNORE_VPN = "key_start_ignore_vpn_1"
    private const val SHUTDOWN_NO_CLIENTS = "key_shutdown_no_clients_1"

    private const val PROXY_BIND_ALL = "key_proxy_bind_all_1"

    private const val SERVER_LIMITS = "key_server_perf_limit_1"
  }
}
