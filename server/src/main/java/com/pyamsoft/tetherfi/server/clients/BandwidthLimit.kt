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

package com.pyamsoft.tetherfi.server.clients

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

@Stable
@Immutable
data class BandwidthLimit(
    val amount: ULong,
    val unit: BandwidthUnit,
) {
  val display by lazy { "$amount ${unit.displayName}" }
}

enum class BandwidthUnit(val displayName: String) {
  BYTE("bytes"),
  KB("KB"),
  MB("MB"),
  GB("GB"),
  TB("TB"),
  PB("PB"),
}
