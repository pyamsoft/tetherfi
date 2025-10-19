/*
 * Copyright 2025 pyamsoft
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

package com.pyamsoft.tetherfi.server.prereq.background

import android.content.Context
import android.net.ConnectivityManager
import androidx.core.content.getSystemService
import com.pyamsoft.pydroid.core.ThreadEnforcer
import com.pyamsoft.pydroid.core.requireNotNull
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class BackgroundDataGuardImpl
@Inject
internal constructor(
    private val context: Context,
    enforcer: ThreadEnforcer,
) : BackgroundDataGuard {

  private val connectivityManager by lazy {
    enforcer.assertOffMainThread()
    context.getSystemService<ConnectivityManager>().requireNotNull()
  }

  override suspend fun canCreateNetwork(): Boolean =
      withContext(context = Dispatchers.Default) {
        val backgroundStatus = connectivityManager.restrictBackgroundStatus

        // We do NOT need to be whitelisted for "always", but we just can NOT be
        // restricted background
        return@withContext backgroundStatus !=
            ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED
      }
}
