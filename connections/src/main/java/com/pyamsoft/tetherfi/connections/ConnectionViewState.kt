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

package com.pyamsoft.tetherfi.connections

import androidx.compose.runtime.Stable
import com.pyamsoft.pydroid.arch.UiViewState
import com.pyamsoft.tetherfi.server.clients.TetherClient
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Stable
interface ConnectionViewState : UiViewState {
  val connections: StateFlow<List<TetherClient>>
  val blocked: StateFlow<Collection<TetherClient>>

  val managingNickName: StateFlow<TetherClient?>
  val managingTransferLimit: StateFlow<TetherClient?>
  val managingBandwidthLimit: StateFlow<TetherClient?>
}

@Stable
class MutableConnectionViewState @Inject internal constructor() : ConnectionViewState {
  override val connections = MutableStateFlow(emptyList<TetherClient>())
  override val blocked = MutableStateFlow<Collection<TetherClient>>(emptySet())

  override val managingNickName = MutableStateFlow<TetherClient?>(null)
  override val managingTransferLimit = MutableStateFlow<TetherClient?>(null)
  override val managingBandwidthLimit = MutableStateFlow<TetherClient?>(null)
}
