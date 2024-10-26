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

package com.pyamsoft.tetherfi.server.proxy.session.tcp

import com.pyamsoft.tetherfi.core.Timber
import com.pyamsoft.tetherfi.server.proxy.SharedProxy

internal abstract class AbstractTcpSessionTransport<Q : ProxyRequest> protected constructor() :
    TcpSessionTransport<Q> {

  private val logTag: String by lazy { proxyType.name }

  protected inline fun debugLog(message: () -> String) {
    Timber.d { "$logTag: ${message()}" }
  }

  protected inline fun warnLog(message: () -> String) {
    Timber.w { "$logTag: ${message()}" }
  }

  protected inline fun errorLog(throwable: Throwable, message: () -> String) {
    Timber.e(throwable) { "$logTag: ${message()}" }
  }

  protected abstract val proxyType: SharedProxy.Type
}
