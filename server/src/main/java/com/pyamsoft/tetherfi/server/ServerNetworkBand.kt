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

package com.pyamsoft.tetherfi.server

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

@Stable
@Immutable
enum class ServerNetworkBand(val enabled: Boolean) {
  // 2.4GHz
  LEGACY(true),

  // 5GHz
  @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.Q)
  MODERN(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q),

  // 6GHz
  @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.BAKLAVA)
  MODERN_6(Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA),
}
