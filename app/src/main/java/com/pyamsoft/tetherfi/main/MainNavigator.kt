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

package com.pyamsoft.tetherfi.main

import androidx.annotation.AnimRes
import androidx.annotation.CheckResult
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.pyamsoft.pydroid.arch.UiSavedStateReader
import com.pyamsoft.pydroid.arch.UiSavedStateWriter
import com.pyamsoft.pydroid.ui.navigator.FragmentNavigator
import com.pyamsoft.tetherfi.R
import com.pyamsoft.tetherfi.activity.ActivityFragment
import com.pyamsoft.tetherfi.error.ErrorFragment
import com.pyamsoft.tetherfi.status.StatusFragment
import javax.inject.Inject

internal class MainNavigator
@Inject
internal constructor(
    activity: MainActivity,
    @IdRes fragmentContainerId: Int,
) : FragmentNavigator<MainView>(activity, fragmentContainerId) {

  override fun onRestoreState(savedInstanceState: UiSavedStateReader) {}

  override fun onSaveState(outState: UiSavedStateWriter) {}

  override fun produceFragmentForScreen(screen: MainView): Fragment =
      when (screen) {
        MainView.Activity -> ActivityFragment.newInstance()
        MainView.Errors -> ErrorFragment.newInstance()
        MainView.Status -> StatusFragment.newInstance()
      }

  override fun performFragmentTransaction(
      container: Int,
      newScreen: Fragment,
      previousScreen: Fragment?
  ) {
    commitNow {
      decideAnimationForPage(newScreen, previousScreen)
      replace(container, newScreen, newScreen::class.java.name)
    }
  }

  companion object {

    private data class FragmentAnimation(
        @AnimRes val enter: Int,
        @AnimRes val exit: Int,
    )

    @CheckResult
    private infix fun Int.then(exit: Int): FragmentAnimation {
      return FragmentAnimation(
          enter = this,
          exit = exit,
      )
    }

    private fun FragmentTransaction.decideAnimationForPage(
        newPage: Fragment,
        oldPage: Fragment?,
    ) {
      val animations =
          when (newPage) {
            is StatusFragment ->
                when (oldPage) {
                  null -> R.anim.fragment_open_enter then R.anim.fragment_open_exit
                  is ActivityFragment, is ErrorFragment ->
                      R.anim.slide_in_left then R.anim.slide_out_right
                  else -> null
                }
            is ActivityFragment ->
                when (oldPage) {
                  null -> R.anim.fragment_open_enter then R.anim.fragment_open_exit
                  is StatusFragment -> R.anim.slide_in_right then R.anim.slide_out_left
                  is ErrorFragment -> R.anim.slide_in_left then R.anim.slide_out_right
                  else -> null
                }
            is ErrorFragment ->
                when (oldPage) {
                  null -> R.anim.fragment_open_enter then R.anim.fragment_open_exit
                  is StatusFragment, is ActivityFragment ->
                      R.anim.slide_in_right then R.anim.slide_out_left
                  else -> null
                }
            else -> null
          }

      if (animations != null) {
        val enter = animations.enter
        val exit = animations.exit
        setCustomAnimations(enter, exit, enter, exit)
      }
    }
  }
}
