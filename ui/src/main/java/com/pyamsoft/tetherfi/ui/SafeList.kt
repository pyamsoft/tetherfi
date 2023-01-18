package com.pyamsoft.tetherfi.ui

import androidx.annotation.CheckResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

data class SafeList<T : Any>(val list: List<T>)

@CheckResult
fun <T : Any> List<T>.safe(): SafeList<T> {
  return SafeList(this)
}

@Composable
@CheckResult
fun <T : Any> List<T>.rememberSafe(): SafeList<T> {
  return remember(this) { this.safe() }
}
