package com.pyamsoft.tetherfi.main

sealed class MainView(val display: String) {
  object Status : MainView("Status")
  object Activity : MainView("Activity")
  object Errors : MainView("Errors")
}
