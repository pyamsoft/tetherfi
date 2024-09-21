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

package com.pyamsoft.tetherfi.server.prereq.location

import android.content.Context
import android.location.LocationManager
import androidx.core.content.getSystemService
import androidx.core.location.LocationManagerCompat
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.pydroid.util.ifNotCancellation
import com.pyamsoft.tetherfi.core.Timber
import com.pyamsoft.tetherfi.server.TweakPreferences
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

@Singleton
internal class AndroidLocationChecker
@Inject
internal constructor(
    private val context: Context,
    private val preferences: TweakPreferences,
) : LocationChecker {

  private val manager by lazy {
    context.applicationContext.getSystemService<LocationManager>().requireNotNull()
  }

  override suspend fun isLocationOn(): Boolean =
      withContext(context = Dispatchers.Default) {
        if (preferences.listenForStartIgnoreLocation().first()) {
          Timber.w { "Ignore Location start blocker" }
          return@withContext true
        }

        try {
          return@withContext LocationManagerCompat.isLocationEnabled(manager)
        } catch (e: Throwable) {
          e.ifNotCancellation {
            // Something went wrong, we are on a low API device (<20?) and do not
            // have location permission? What?
            Timber.e(e) { "Error trying to access isLocationEnabled" }
            return@withContext false
          }
        }
      }
}
