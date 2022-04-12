package com.pyamsoft.widefi.main

import android.os.Bundle
import androidx.annotation.CheckResult
import com.pyamsoft.pydroid.ui.navigator.Navigator

sealed class MainView(val display: String) {
  object Status : MainView("Status")
  object Activity : MainView("Activity")
  object Errors : MainView("Errors")

  @CheckResult
  fun asScreen(): Navigator.Screen<MainView> {
    val self = this
    return object : Navigator.Screen<MainView> {
      override val arguments: Bundle? = null
      override val screen: MainView = self
    }
  }
}
