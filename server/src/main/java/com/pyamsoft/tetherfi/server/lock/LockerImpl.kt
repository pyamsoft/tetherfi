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

package com.pyamsoft.tetherfi.server.lock

import com.pyamsoft.tetherfi.server.ServerInternalApi
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
    @param:ServerInternalApi private val lockers: MutableSet<Locker>
) : Locker {
  override suspend fun createLock(): Locker.Lock =
      withContext(context = Dispatchers.Default) {
        return@withContext Lock(locks = lockers.map { it.createLock() })
      }

  private class Lock(private val locks: Collection<Locker.Lock>) : Locker.Lock {
    override suspend fun acquire(): Locker.Lock.Releaser =
        withContext(context = Dispatchers.Default) {
          val acquiredLocks = locks.map { it.acquire() }
          return@withContext Locker.Lock.Releaser { acquiredLocks.forEach { it.release() } }
        }

    override suspend fun release() =
        withContext(context = Dispatchers.Default) {
          withContext(context = NonCancellable) { locks.forEach { it.release() } }
        }
  }
}
