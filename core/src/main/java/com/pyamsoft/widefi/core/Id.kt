package com.pyamsoft.widefi.core

import androidx.annotation.CheckResult
import java.util.*

@CheckResult
fun generateRandomId(): String {
  return UUID.randomUUID().toString()
}
