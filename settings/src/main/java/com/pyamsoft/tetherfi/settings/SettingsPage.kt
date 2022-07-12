package com.pyamsoft.tetherfi.settings

sealed class SettingsPage(val name: String) {
  object Settings : SettingsPage("Settings")
}
