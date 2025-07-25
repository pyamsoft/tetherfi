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

package com.pyamsoft.tetherfi.main

import androidx.compose.runtime.Stable
import com.pyamsoft.pydroid.arch.UiViewState
import com.pyamsoft.pydroid.ui.theme.Theming
import com.pyamsoft.tetherfi.core.ActivityScope
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Stable
interface ThemeViewState : UiViewState {
  val mode: StateFlow<Theming.Mode>
  val isMaterialYou: StateFlow<Boolean>
}

@Stable
@ActivityScope
class MutableThemeViewState @Inject internal constructor() : ThemeViewState {
  override val mode = MutableStateFlow(Theming.Mode.SYSTEM)
  override val isMaterialYou = MutableStateFlow(false)
}
