package com.pyamsoft.tetherfi.main

sealed class MainView(val name: String) {
  object Status : MainView("Hotspot")
  object Info : MainView("How To")
}
