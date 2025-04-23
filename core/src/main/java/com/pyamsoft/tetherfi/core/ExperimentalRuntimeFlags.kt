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

import kotlinx.coroutines.flow.Flow

/** Run-time feature flags */
interface ExperimentalRuntimeFlags {

  /**
   * Fake an error during the socket builder launch{} hook
   *
   * Simulates what happens when a low power device runs out of memory/FS handles
   */
  val isSocketBuilderOOMServer: Flow<Boolean>

  /**
   * Fake an error building the socket
   *
   * Simulates what happens when we can't allocate our own logic but are granted an FS handle
   */
  val isSocketBuilderOOMClient: Flow<Boolean>

  /**
   * SOCKS proxy
   *
   * Adapted from https://github.com/torsm/ktor-socks
   */
  val isSocksProxyEnabled: Flow<Boolean>

  object Defaults {
    const val IS_SOCKS_PROXY_ENABLED_INITIAL_STATE: Boolean = false
  }
}
