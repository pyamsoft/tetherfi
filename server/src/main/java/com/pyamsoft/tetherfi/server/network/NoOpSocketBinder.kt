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

package com.pyamsoft.tetherfi.server.network

import com.pyamsoft.tetherfi.core.Timber
import com.pyamsoft.tetherfi.server.ServerInternalApi
import javax.inject.Inject
import javax.inject.Singleton

// https://github.com/pyamsoft/tetherfi/issues/154
// https://github.com/pyamsoft/tetherfi/issues/331
@Singleton
@ServerInternalApi
internal class NoOpSocketBinder
@Inject
internal constructor() : SocketBinder {

    override suspend fun withMobileDataNetworkActive(
        block: suspend (SocketBinder.NetworkBinder) -> Unit
    ) {
        Timber.d { "Using currently active network for proxy connections..." }
        block(NOOP_BOUND)
    }

    companion object {
        private val NOOP_BOUND: SocketBinder.NetworkBinder =
            SocketBinder.NetworkBinder { Timber.d { "Not binding to any socket" } }
    }
}
