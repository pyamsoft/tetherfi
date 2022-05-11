/*
 * Copyright 2021 Peter Kenji Yamanaka
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

package com.pyamsoft.widefi.settings

import androidx.annotation.CheckResult
import androidx.annotation.IdRes
import com.pyamsoft.pydroid.ui.navigator.Navigator
import dagger.Binds
import dagger.BindsInstance
import dagger.Module
import dagger.Subcomponent
import javax.inject.Named

@Subcomponent(modules = [SettingsComponent.SettingsModule::class])
internal interface SettingsComponent {

  fun inject(dialog: SettingsDialog)

  @Subcomponent.Factory
  interface Factory {

    @CheckResult
    fun create(
        @BindsInstance dialog: SettingsDialog,
        @BindsInstance @IdRes @Named("settings_container") fragmentContainerId: Int,
    ): SettingsComponent
  }

  @Module
  abstract class SettingsModule {

    @Binds
    @CheckResult
    internal abstract fun bindNavigator(impl: SettingsNavigator): Navigator<SettingsPage>
  }
}
