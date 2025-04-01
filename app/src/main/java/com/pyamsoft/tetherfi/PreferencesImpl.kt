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
import androidx.annotation.CheckResult
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.preference.PreferenceManager
import com.pyamsoft.pydroid.core.ThreadEnforcer
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
) :
    StatusPreferences,
    ProxyPreferences,
    InAppRatingPreferences,
    TweakPreferences,
    ExpertPreferences,
    WifiPreferences {

  private val Context.dataStore by
      preferencesDataStore(
          name = "tetherfi_preferences",
          produceMigrations = {
            listOf(
                // NOTE(Peter): Since our shared preferences was the DEFAULT process one, loading up
                //              a migration without specifying all keys will also migrate
                //              PYDROID SPECIFIC PREFERENCES which is what we do NOT want to do.
                //              We instead maintain ONLY a list of the known app preference keys
                SharedPreferencesMigration(
                    keysToMigrate =
                        setOf(
                            SSID.name,
                            PASSWORD.name,
                            HTTP_PORT.name,
                            SOCKS_PORT.name,
                            NETWORK_BAND.name,
                            IN_APP_HOTSPOT_USED.name,
                            IN_APP_DEVICES_CONNECTED.name,
                            IN_APP_APP_OPENED.name,
                            IN_APP_RATING_SHOWN_VERSION.name,
                            START_IGNORE_VPN.name,
                            START_IGNORE_LOCATION.name,
                            SHUTDOWN_NO_CLIENTS.name,
                            SERVER_LIMITS.name,
                            KEEP_SCREEN_ON.name,
                            BROADCAST_TYPE.name,
                            PREFERRED_NETWORK.name,
                            SOCKET_TIMEOUT.name,
                        ),
                    produceSharedPreferences = {
                      PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
                    },
                ),
            )
          },
      )

  private val preferences by lazy {
    context.applicationContext.dataStore
  }

  // Keep this lazy so that the fallback password is always the same
  private val fallbackPassword by lazy { PasswordGenerator.generate() }

  private val scope by lazy {
    CoroutineScope(
        context = SupervisorJob() + Dispatchers.IO + CoroutineName(this::class.java.name),
    )
  }

  private inline fun setPreference(crossinline block: suspend (MutablePreferences) -> Unit) {
    scope.launch(context = Dispatchers.IO) {
      enforcer.assertOffMainThread()

      preferences.edit { settings ->
        enforcer.assertOffMainThread()
        block(settings)
      }
    }
  }

  @CheckResult
  private fun Int.isInAppRatingAlreadyShown(): Boolean {
    enforcer.assertOffMainThread()

    val self = this
    return self > 0 && self == BuildConfig.VERSION_CODE
  }

  @CheckResult
  private fun getInAppRatingShownVersion(preferences: Preferences): Int =
      preferences[IN_APP_RATING_SHOWN_VERSION] ?: 0

  override fun listenForSsidChanges(): Flow<String> =
      preferences.data.map { it[SSID] ?: ServerDefaults.WIFI_SSID }.flowOn(context = Dispatchers.IO)

  override fun setSsid(ssid: String) = setPreference { it[SSID] = ssid }

  override fun listenForPasswordChanges(): Flow<String> =
      preferences.data
          .map { settings ->
            val existingPassword = settings[PASSWORD]

            if (existingPassword == null) {
              // Side effect, create default password
              preferences.edit { it[PASSWORD] = fallbackPassword }
              return@map fallbackPassword
            }

            return@map existingPassword
          }
          .flowOn(context = Dispatchers.IO)

  override fun setPassword(password: String) = setPreference { it[PASSWORD] = password }

  override fun listenForHttpPortChanges(): Flow<Int> =
      preferences.data
          .map { it[HTTP_PORT] ?: ServerDefaults.HTTP_PORT }
          .flowOn(context = Dispatchers.IO)

  override fun setHttpPort(port: Int) = setPreference { it[HTTP_PORT] = port }

  override fun listenForSocksPortChanges(): Flow<Int> =
      preferences.data
          .map { it[SOCKS_PORT] ?: ServerDefaults.SOCKS_PORT }
          .flowOn(context = Dispatchers.IO)

  override fun setSocksPort(port: Int) = setPreference { it[SOCKS_PORT] = port }

  override fun listenForNetworkBandChanges(): Flow<ServerNetworkBand> =
      preferences.data
          .map { it[NETWORK_BAND] ?: ServerDefaults.WIFI_NETWORK_BAND.name }
          .map { ServerNetworkBand.valueOf(it) }
          .flowOn(context = Dispatchers.IO)

  override fun setNetworkBand(band: ServerNetworkBand) = setPreference {
    it[NETWORK_BAND] = band.name
  }

  override fun listenForStartIgnoreVpn(): Flow<Boolean> =
      preferences.data
          .map { it[START_IGNORE_VPN] ?: DEFAULT_START_IGNORE_VPN }
          .flowOn(context = Dispatchers.IO)

  override fun setStartIgnoreVpn(ignore: Boolean) = setPreference { it[START_IGNORE_VPN] = ignore }

  override fun listenForStartIgnoreLocation(): Flow<Boolean> =
      preferences.data
          .map { it[START_IGNORE_LOCATION] ?: DEFAULT_START_IGNORE_LOCATION }
          .flowOn(context = Dispatchers.IO)

  override fun setStartIgnoreLocation(ignore: Boolean) = setPreference {
    it[START_IGNORE_LOCATION] = ignore
  }

  override fun listenForShutdownWithNoClients(): Flow<Boolean> =
      preferences.data
          .map { it[SHUTDOWN_NO_CLIENTS] ?: DEFAULT_SHUTDOWN_NO_CLIENTS }
          .flowOn(context = Dispatchers.IO)

  override fun setShutdownWithNoClients(shutdown: Boolean) = setPreference {
    it[SHUTDOWN_NO_CLIENTS] = shutdown
  }

  override fun listenForKeepScreenOn(): Flow<Boolean> =
      preferences.data
          .map { it[KEEP_SCREEN_ON] ?: DEFAULT_KEEP_SCREEN_ON }
          .flowOn(context = Dispatchers.IO)

  override fun setKeepScreenOn(keep: Boolean) = setPreference { it[KEEP_SCREEN_ON] = keep }

  override fun listenForBroadcastType(): Flow<BroadcastType> =
      preferences.data
          .map { it[BROADCAST_TYPE] ?: BroadcastType.WIFI_DIRECT.name }
          .map { BroadcastType.valueOf(it) }
          .flowOn(context = Dispatchers.IO)

  override fun setBroadcastType(type: BroadcastType) = setPreference {
    it[BROADCAST_TYPE] = type.name
  }

  override fun listenForPreferredNetwork(): Flow<PreferredNetwork> =
      preferences.data
          .map { it[PREFERRED_NETWORK] ?: PreferredNetwork.NONE.name }
          .map { PreferredNetwork.valueOf(it) }
          .flowOn(context = Dispatchers.IO)

  override fun setPreferredNetwork(network: PreferredNetwork) = setPreference {
    it[PREFERRED_NETWORK] = network.name
  }

  override fun listenShowInAppRating(): Flow<Boolean> =
      combineTransform(
              preferences.data.map { it[IN_APP_HOTSPOT_USED] ?: 0 },
              preferences.data.map { it[IN_APP_DEVICES_CONNECTED] ?: 0 },
              preferences.data.map { it[IN_APP_APP_OPENED] ?: 0 },
              preferences.data.map { it[IN_APP_RATING_SHOWN_VERSION] ?: 0 },
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
                preferences.edit { settings ->
                  // Reset the previous flags
                  settings[IN_APP_APP_OPENED] = 0
                  settings[IN_APP_HOTSPOT_USED] = 0
                  settings[IN_APP_DEVICES_CONNECTED] = 0

                  // And mark the latest version
                  settings[IN_APP_RATING_SHOWN_VERSION] = BuildConfig.VERSION_CODE
                }
              }
            }
          }
          // Need this or we run on the main thread
          .flowOn(context = Dispatchers.IO)

  private inline fun updateInt(
      preferences: MutablePreferences,
      key: Preferences.Key<Int>,
      mutate: (Int) -> Int,
  ) {
    val current = preferences[key] ?: 0
    preferences[key] = mutate(current)
  }

  override fun markHotspotUsed() = setPreference { settings ->
    val version = getInAppRatingShownVersion(settings)
    if (!version.isInAppRatingAlreadyShown()) {
      updateInt(settings, IN_APP_HOTSPOT_USED) { it + 1 }
    }
  }

  override fun markAppOpened() = setPreference { settings ->
    val version = getInAppRatingShownVersion(settings)
    if (!version.isInAppRatingAlreadyShown()) {
      updateInt(settings, IN_APP_APP_OPENED) { it + 1 }
    }
  }

  override fun markDeviceConnected() = setPreference { settings ->
    val version = getInAppRatingShownVersion(settings)
    if (!version.isInAppRatingAlreadyShown()) {
      updateInt(settings, IN_APP_DEVICES_CONNECTED) { it + 1 }
    }
  }

  override fun listenForPerformanceLimits(): Flow<ServerPerformanceLimit> =
      preferences.data
          .map { it[SERVER_LIMITS] ?: ServerPerformanceLimit.Defaults.BOUND_3N_CPU.coroutineLimit }
          .map { ServerPerformanceLimit.create(it) }

  override fun setServerPerformanceLimit(limit: ServerPerformanceLimit) = setPreference {
    it[SERVER_LIMITS] = limit.coroutineLimit
  }

  override fun listenForSocketTimeout(): Flow<ServerSocketTimeout> =
      preferences.data
          .map {
            it[SOCKET_TIMEOUT]
                ?: ServerSocketTimeout.Defaults.BALANCED.timeoutDuration.inWholeSeconds
          }
          .map { ServerSocketTimeout.create(it) }

  override fun setSocketTimeout(limit: ServerSocketTimeout) = setPreference { settings ->
    val seconds =
        if (limit.timeoutDuration.isInfinite()) -1 else limit.timeoutDuration.inWholeSeconds
    settings[SOCKET_TIMEOUT] = seconds
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

    private val SSID = stringPreferencesKey("key_ssid_1")
    private val PASSWORD = stringPreferencesKey("key_password_1")
    private val HTTP_PORT = intPreferencesKey("key_port_1")
    private val SOCKS_PORT = intPreferencesKey("key_socks_port_1")
    private val NETWORK_BAND = stringPreferencesKey("key_network_band_1")

    private val IN_APP_HOTSPOT_USED = intPreferencesKey("key_in_app_hotspot_used_1")
    private val IN_APP_DEVICES_CONNECTED = intPreferencesKey("key_in_app_devices_connected_1")
    private val IN_APP_APP_OPENED = intPreferencesKey("key_in_app_app_opened_1")

    private val IN_APP_RATING_SHOWN_VERSION = intPreferencesKey("key_in_app_rating_shown_version")

    private val START_IGNORE_VPN = booleanPreferencesKey("key_start_ignore_vpn_1")
    private const val DEFAULT_START_IGNORE_VPN = false

    private val START_IGNORE_LOCATION = booleanPreferencesKey("key_start_ignore_location_1")
    private const val DEFAULT_START_IGNORE_LOCATION = false

    private val SHUTDOWN_NO_CLIENTS = booleanPreferencesKey("key_shutdown_no_clients_1")
    private const val DEFAULT_SHUTDOWN_NO_CLIENTS = false

    private val SERVER_LIMITS = intPreferencesKey("key_server_perf_limit_1")

    private val KEEP_SCREEN_ON = booleanPreferencesKey("key_keep_screen_on_1")
    private const val DEFAULT_KEEP_SCREEN_ON = false

    private val BROADCAST_TYPE = stringPreferencesKey("key_broadcast_type_1")

    private val PREFERRED_NETWORK = stringPreferencesKey("key_preferred_network_1")

    private val SOCKET_TIMEOUT = longPreferencesKey("key_socket_timeout_1")
  }
}
