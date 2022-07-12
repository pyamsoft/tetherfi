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

package com.pyamsoft.tetherfi.settings

import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import com.pyamsoft.pydroid.arch.UiSavedStateReader
import com.pyamsoft.pydroid.arch.UiSavedStateWriter
import com.pyamsoft.pydroid.ui.navigator.FragmentNavigator
import javax.inject.Inject
import javax.inject.Named

internal class SettingsNavigator
@Inject
internal constructor(
    dialog: SettingsDialog,
    @IdRes @Named("settings_container") fragmentContainerId: Int,
) :
    FragmentNavigator<SettingsPage>(
        lifecycleOwner = dialog.viewLifecycleOwner,
        fragmentManager = dialog.childFragmentManager,
        fragmentContainerId = fragmentContainerId,
    ) {

  override fun onRestoreState(savedInstanceState: UiSavedStateReader) {}

  override fun onSaveState(outState: UiSavedStateWriter) {}

  override fun produceFragmentForScreen(screen: SettingsPage): Fragment =
      when (screen) {
        is SettingsPage.Settings -> AppSettings.newInstance()
      }

  override fun performFragmentTransaction(
      container: Int,
      newScreen: Fragment,
      previousScreen: Fragment?
  ) {
    commitNow { replace(container, newScreen, newScreen::class.java.name) }
  }
}
