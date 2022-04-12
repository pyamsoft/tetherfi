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

package com.pyamsoft.widefi.main

import androidx.annotation.CheckResult
import androidx.annotation.IdRes
import com.pyamsoft.pydroid.ui.navigator.Navigator
import com.pyamsoft.widefi.activity.ActivityComponent
import com.pyamsoft.widefi.core.ActivityScope
import com.pyamsoft.widefi.error.ErrorComponent
import com.pyamsoft.widefi.status.StatusComponent
import dagger.Binds
import dagger.BindsInstance
import dagger.Module
import dagger.Subcomponent

@ActivityScope
@Subcomponent(modules = [MainComponent.MainModule::class])
internal interface MainComponent {

  @CheckResult fun plusStatus(): StatusComponent.Factory

  @CheckResult fun plusActivity(): ActivityComponent.Factory

  @CheckResult fun plusError(): ErrorComponent.Factory

  fun inject(activity: MainActivity)

  @Subcomponent.Factory
  interface Factory {

    @CheckResult
    fun create(
        @BindsInstance activity: MainActivity,
        @BindsInstance @IdRes fragmentContainerId: Int,
    ): MainComponent
  }

  @Module
  abstract class MainModule {

    @Binds
    @CheckResult
    internal abstract fun bindNavigator(impl: MainNavigator): Navigator<MainView>
  }
}
