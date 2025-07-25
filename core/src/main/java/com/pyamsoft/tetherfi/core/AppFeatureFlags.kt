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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AppFeatureFlags @Inject internal constructor() : FeatureFlags {

  companion object {

    /** Set this false to turn off all feature flags */
    private const val IS_FEATURE_FLAGS_ENABLED = true

    @CheckResult
    @Suppress("unused")
    private inline fun flag(block: () -> Boolean): Boolean {
      return IS_FEATURE_FLAGS_ENABLED && block()
    }
  }
}
