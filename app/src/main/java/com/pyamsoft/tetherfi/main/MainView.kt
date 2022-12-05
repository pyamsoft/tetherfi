package com.pyamsoft.tetherfi.main

sealed class MainView(val display: String) {
  object Status : MainView("Status")
}
