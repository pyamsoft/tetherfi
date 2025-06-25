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

package com.pyamsoft.tetherfi.core

import androidx.annotation.CheckResult
import kotlinx.coroutines.flow.Flow

/** Keep track of how the user does things in-app so that we can eventually prompt for a rating */
interface InAppRatingPreferences {

  /** Watch to show the rating */
  @CheckResult fun listenShowInAppRating(): Flow<Boolean>

  /** How many times has the user turned the hotspot on */
  fun markHotspotUsed()

  /** How many times is the app opened (onStart) */
  fun markAppOpened()

  /** How many times have devices connected to the hotspot */
  fun markDeviceConnected()
}
