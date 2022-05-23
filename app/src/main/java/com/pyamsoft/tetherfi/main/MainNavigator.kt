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

import android.os.Bundle
import androidx.annotation.CheckResult
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.pyamsoft.pydroid.arch.UiSavedStateReader
import com.pyamsoft.pydroid.arch.UiSavedStateWriter
import com.pyamsoft.pydroid.ui.navigator.FragmentNavigator
import com.pyamsoft.pydroid.ui.navigator.Navigator
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

  override fun restoreState(savedInstanceState: UiSavedStateReader) {
    val s = savedInstanceState.get<String>(KEY_SCREEN_ID)
    if (s != null) {
      val restored =
          when (s) {
            MainView.Status::class.java.name -> MainView.Status
            MainView.Errors::class.java.name -> MainView.Errors
            MainView.Activity::class.java.name -> MainView.Activity
            else -> throw IllegalArgumentException("Unable to restore screen: $s")
          }
      updateCurrentScreen(restored)
    }
  }

  override fun saveState(outState: UiSavedStateWriter) {
    val s = currentScreen()
    if (s != null) {
      outState.put(KEY_SCREEN_ID, s::class.java.name)
    } else {
      outState.remove<String>(KEY_SCREEN_ID)
    }
  }

  override fun performFragmentTransaction(
      container: Int,
      data: FragmentTag,
      newScreen: Navigator.Screen<MainView>,
      previousScreen: MainView?
  ) {
    commitNow {
      decideAnimationForPage(newScreen.screen, previousScreen)
      replace(container, data.fragment(newScreen.arguments), data.tag)
    }
  }

  override fun provideFragmentTagMap(): Map<MainView, FragmentTag> {
    return mapOf(
        MainView.Status to createFragmentTag("StatusFragment") { StatusFragment.newInstance() },
        MainView.Activity to
            createFragmentTag("ActivityFragment") { ActivityFragment.newInstance() },
        MainView.Errors to createFragmentTag("ErrorFragment") { ErrorFragment.newInstance() },
    )
  }

  companion object {

    private const val KEY_SCREEN_ID = "key_screen_id"

    private fun FragmentTransaction.decideAnimationForPage(newPage: MainView, oldPage: MainView?) {
      val animations =
          if (oldPage == null) R.anim.fragment_open_enter to R.anim.fragment_open_exit
          else {
            when (newPage) {
              is MainView.Activity ->
                  when (oldPage) {
                    is MainView.Status -> R.anim.slide_in_right to R.anim.slide_out_left
                    is MainView.Errors -> R.anim.slide_in_left to R.anim.slide_out_right
                    is MainView.Activity -> null
                  }
              is MainView.Status ->
                  when (oldPage) {
                    is MainView.Activity, is MainView.Errors ->
                        R.anim.slide_in_left to R.anim.slide_out_right
                    is MainView.Status -> null
                  }
              is MainView.Errors ->
                  when (oldPage) {
                    is MainView.Status, is MainView.Activity ->
                        R.anim.slide_in_right to R.anim.slide_out_left
                    is MainView.Errors -> null
                  }
            }
          }

      if (animations != null) {
        val (enter, exit) = animations
        setCustomAnimations(enter, exit, enter, exit)
      }
    }

    @JvmStatic
    @CheckResult
    private inline fun createFragmentTag(
        tag: String,
        crossinline fragment: (arguments: Bundle?) -> Fragment,
    ): FragmentTag {
      return object : FragmentTag {
        override val fragment: (arguments: Bundle?) -> Fragment = { fragment(it) }
        override val tag: String = tag
      }
    }
  }
}
