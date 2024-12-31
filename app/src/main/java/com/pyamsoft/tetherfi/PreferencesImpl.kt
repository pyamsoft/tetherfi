/*
 * Copyright 2024 pyamsoft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
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
import com.pyamsoft.pydroid.util.preferenceLongFlow
import com.pyamsoft.pydroid.util.preferenceStringFlow
import com.pyamsoft.tetherfi.core.InAppRatingPreferences
import com.pyamsoft.tetherfi.core.Timber
import com.pyamsoft.tetherfi.server.ExpertPreferences
import com.pyamsoft.tetherfi.server.ProxyPreferences
import com.pyamsoft.tetherfi.server.ServerDefaults
import com.pyamsoft.tetherfi.server.ServerNetworkBand
import com.pyamsoft.tetherfi.server.ServerPerformanceLimit
import com.pyamsoft.tetherfi.server.ServerSocketTimeout
import com.pyamsoft.tetherfi.server.StatusPreferences
import com.pyamsoft.tetherfi.server.TweakPreferences
import com.pyamsoft.tetherfi.server.WifiPreferences
import com.pyamsoft.tetherfi.server.broadcast.BroadcastType
import com.pyamsoft.tetherfi.server.network.PreferredNetwork
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
internal class PreferencesImpl
@Inject
internal constructor(
    private val enforcer: ThreadEnforcer,
    context: Context,
) :
    StatusPreferences,
    ProxyPreferences,
    InAppRatingPreferences,
    TweakPreferences,
    ExpertPreferences,
    WifiPreferences {

  private val preferences by lazy {
    enforcer.assertOffMainThread()
    PreferenceManager.getDefaultSharedPreferences(context.applicationContext).also {
      removeOldPreferences(it)
    }
  }

  // Keep this lazy so that the fallback password is always the same
  private val fallbackPassword by lazy { PasswordGenerator.generate() }

  private val scope by lazy {
    CoroutineScope(
        context = SupervisorJob() + Dispatchers.IO + CoroutineName(this::class.java.name),
    )
  }

  private fun removeOldPreferences(preferences: SharedPreferences) {
    preferences.edit { OLD_PREFERENCES.forEach { remove(it) } }
  }

  private inline fun setPreference(crossinline block: SharedPreferences.Editor.() -> Unit) {
    scope.launch {
      enforcer.assertOffMainThread()
      preferences.edit(action = block)
    }
  }

  private inline fun updatePreference(crossinline block: SharedPreferences.() -> Unit) {
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

  override fun listenForSsidChanges(): Flow<String> =
      preferenceStringFlow(SSID, ServerDefaults.WIFI_SSID) { preferences }
          .flowOn(context = Dispatchers.IO)

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
          .flowOn(context = Dispatchers.IO)

  override fun setPassword(password: String) = setPreference {
    preferences.updatePassword(password)
  }

  override fun listenForHttpPortChanges(): Flow<Int> =
      preferenceIntFlow(HTTP_PORT, ServerDefaults.HTTP_PORT) { preferences }
          .flowOn(context = Dispatchers.IO)

  override fun setHttpPort(port: Int) = setPreference { putInt(HTTP_PORT, port) }

  override fun listenForSocksPortChanges(): Flow<Int> =
      preferenceIntFlow(SOCKS_PORT, ServerDefaults.SOCKS_PORT) { preferences }
          .flowOn(context = Dispatchers.IO)

  override fun setSocksPort(port: Int) = setPreference { putInt(SOCKS_PORT, port) }

  override fun listenForNetworkBandChanges(): Flow<ServerNetworkBand> =
      preferenceStringFlow(NETWORK_BAND, ServerDefaults.WIFI_NETWORK_BAND.name) { preferences }
          .map { ServerNetworkBand.valueOf(it) }
          .flowOn(context = Dispatchers.IO)

  override fun setNetworkBand(band: ServerNetworkBand) = setPreference {
    putString(NETWORK_BAND, band.name)
  }

  override fun listenForStartIgnoreVpn(): Flow<Boolean> =
      preferenceBooleanFlow(START_IGNORE_VPN, false) { preferences }
          .flowOn(context = Dispatchers.IO)

  override fun setStartIgnoreVpn(ignore: Boolean) = setPreference {
    putBoolean(START_IGNORE_VPN, ignore)
  }

  override fun listenForStartIgnoreLocation(): Flow<Boolean> =
      preferenceBooleanFlow(START_IGNORE_LOCATION, false) { preferences }
          .flowOn(context = Dispatchers.IO)

  override fun setStartIgnoreLocation(ignore: Boolean) = setPreference {
    putBoolean(START_IGNORE_LOCATION, ignore)
  }

  override fun listenForShutdownWithNoClients(): Flow<Boolean> =
      preferenceBooleanFlow(SHUTDOWN_NO_CLIENTS, false) { preferences }
          .flowOn(context = Dispatchers.IO)

  override fun setShutdownWithNoClients(shutdown: Boolean) = setPreference {
    putBoolean(SHUTDOWN_NO_CLIENTS, shutdown)
  }

  override fun listenForKeepScreenOn(): Flow<Boolean> =
      preferenceBooleanFlow(KEEP_SCREEN_ON, false) { preferences }.flowOn(context = Dispatchers.IO)

  override fun setKeepScreenOn(keep: Boolean) = setPreference { putBoolean(KEEP_SCREEN_ON, keep) }

  override fun listenForBroadcastType(): Flow<BroadcastType> =
      preferenceStringFlow(BROADCAST_TYPE, BroadcastType.WIFI_DIRECT.name) { preferences }
          .map { BroadcastType.valueOf(it) }
          .flowOn(context = Dispatchers.IO)

  override fun setBroadcastType(type: BroadcastType) = setPreference {
    putString(BROADCAST_TYPE, type.name)
  }

  override fun listenForPreferredNetwork(): Flow<PreferredNetwork> =
      preferenceStringFlow(PREFERRED_NETWORK, PreferredNetwork.NONE.name) { preferences }
          .map { PreferredNetwork.valueOf(it) }
          .flowOn(context = Dispatchers.IO)

  override fun setPreferredNetwork(network: PreferredNetwork) = setPreference {
    putString(PREFERRED_NETWORK, network.name)
  }

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
          .flowOn(context = Dispatchers.IO)

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

  override fun listenForSocketTimeout(): Flow<ServerSocketTimeout> =
      preferenceLongFlow(
              SOCKET_TIMEOUT,
              ServerSocketTimeout.Defaults.BALANCED.timeoutDuration.inWholeSeconds) {
                preferences
              }
          .map { ServerSocketTimeout.create(it) }

  override fun setSocketTimeout(limit: ServerSocketTimeout) = setPreference {
    val seconds =
        if (limit.timeoutDuration.isInfinite()) -1 else limit.timeoutDuration.inWholeSeconds
    putLong(SOCKET_TIMEOUT, seconds)
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
    private val OLD_PREFERENCES =
        listOf(
            // PROXY_BIND_ALL: Removed in 41
            "key_proxy_bind_all_1",

            // YOLO_MODE: Made default in 41, removed in 42
            "key_proxy_yolo_mode_1",

            // TCP socket timeout was default OFF in <44
            // Made default ON in 44 and replaced key with _2
            "key_tcp_socket_timeout_1",

            // Removed wake lock and wifi lock in version 45
            "key_wake_lock_1",
            "key_wifi_lock_1",

            // Removed TCP socket timeout in version 45
            "key_tcp_socket_timeout_2",
        )

    private const val SSID = "key_ssid_1"
    private const val PASSWORD = "key_password_1"
    private const val HTTP_PORT = "key_port_1"
    private const val SOCKS_PORT = "key_socks_port_1"
    private const val NETWORK_BAND = "key_network_band_1"

    private const val IN_APP_HOTSPOT_USED = "key_in_app_hotspot_used_1"
    private const val IN_APP_DEVICES_CONNECTED = "key_in_app_devices_connected_1"
    private const val IN_APP_APP_OPENED = "key_in_app_app_opened_1"

    private const val IN_APP_RATING_SHOWN_VERSION = "key_in_app_rating_shown_version"

    private const val START_IGNORE_VPN = "key_start_ignore_vpn_1"
    private const val START_IGNORE_LOCATION = "key_start_ignore_location_1"
    private const val SHUTDOWN_NO_CLIENTS = "key_shutdown_no_clients_1"

    private const val SERVER_LIMITS = "key_server_perf_limit_1"

    private const val KEEP_SCREEN_ON = "key_keep_screen_on_1"

    private const val BROADCAST_TYPE = "key_broadcast_type_1"

    private const val PREFERRED_NETWORK = "key_preferred_network_1"

    private const val SOCKET_TIMEOUT = "key_socket_timeout_1"
  }
}
