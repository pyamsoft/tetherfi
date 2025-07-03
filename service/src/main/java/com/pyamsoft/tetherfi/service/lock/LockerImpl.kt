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

package com.pyamsoft.tetherfi.service.lock

import com.pyamsoft.tetherfi.service.ServiceInternalApi
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

@Singleton
internal class LockerImpl
@Inject
internal constructor(
    // Need to use MutableSet instead of Set because of Java -> Kotlin fun.
    @param:ServiceInternalApi private val lockers: MutableSet<Locker>,
) : Locker {
  override suspend fun acquire() =
      withContext(context = NonCancellable) {
        withContext(context = Dispatchers.Default) { lockers.forEach { it.acquire() } }
      }

  override suspend fun release() =
      withContext(context = NonCancellable) {
        withContext(context = Dispatchers.Default) { lockers.forEach { it.release() } }
      }
}
