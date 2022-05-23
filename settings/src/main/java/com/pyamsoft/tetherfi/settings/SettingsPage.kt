package com.pyamsoft.tetherfi.settings

import android.os.Bundle
import androidx.annotation.CheckResult
import com.pyamsoft.pydroid.ui.navigator.Navigator

sealed class SettingsPage(val name: String) {
  object Settings : SettingsPage("Settings")

  @CheckResult
  fun asScreen(): Navigator.Screen<SettingsPage> {
    val self = this
    return object : Navigator.Screen<SettingsPage> {
      override val arguments: Bundle? = null
      override val screen: SettingsPage = self
    }
  }
}
