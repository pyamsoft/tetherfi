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

import androidx.annotation.CheckResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@ConsistentCopyVisibility
internal data class BandwidthLimiter internal constructor(
    private val mutex: Mutex = Mutex(),
    private val amount: MutableStateFlow<Long> = MutableStateFlow(0L),
) {

    @CheckResult
    suspend fun updateAndGet(count: Int): Long {
        return mutex.withLock { amount.updateAndGet { it + count } }
    }

    suspend fun reset() {
        mutex.withLock { amount.value = 0L }
    }
}