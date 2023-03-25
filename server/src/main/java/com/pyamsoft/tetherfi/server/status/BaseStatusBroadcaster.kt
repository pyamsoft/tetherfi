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

package com.pyamsoft.tetherfi.server.status

import kotlinx.coroutines.flow.MutableStateFlow

abstract class BaseStatusBroadcaster protected constructor() : StatusBroadcast {

  private val state = MutableStateFlow<RunningStatus>(RunningStatus.NotRunning)

  final override fun set(status: RunningStatus) {
    val old = state.value
    if (old != status) {
      state.value = status
    }
  }

  final override suspend fun onStatus(block: suspend (RunningStatus) -> Unit) {
    state.collect { block(it) }
  }

  final override fun get(): RunningStatus {
    return state.value
  }
}
