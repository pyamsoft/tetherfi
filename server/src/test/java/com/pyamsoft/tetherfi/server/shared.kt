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

package com.pyamsoft.tetherfi.server

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

const val HOSTNAME = "127.0.0.1"

/**
 * This is like runTest, but it does not skip delay() calls.
 *
 * We need to actually be able to delay, since server spinup takes a "little bit" of time.
 */
inline fun runBlockingWithDelays(
    timeout: Duration = 10.seconds,
    crossinline block: suspend CoroutineScope.() -> Unit,
): Unit = runBlocking {
  try {
    withTimeout(timeout) { block() }
  } catch (e: Throwable) {
    e.printStackTrace()
    throw e
  }
}
