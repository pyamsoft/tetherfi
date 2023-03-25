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

package com.pyamsoft.tetherfi.server

import androidx.annotation.CheckResult
import com.pyamsoft.tetherfi.server.proxy.SharedProxy

internal enum class ProxyDebug {

  /** No debug messages */
  NONE,

  /** TCP related messages */
  TCP,

  /** UDP related messages */
  UDP,

  /** All debug messages */
  ALL;

  /** We have to be allowed to debug this type */
  @CheckResult
  fun isAllowed(type: SharedProxy.Type): Boolean {
    if (this == NONE) {
      return false
    }

    if (this == ALL) {
      return true
    }

    return when (type) {
      SharedProxy.Type.TCP -> this == TCP
      SharedProxy.Type.UDP -> this == UDP
    }
  }
}
