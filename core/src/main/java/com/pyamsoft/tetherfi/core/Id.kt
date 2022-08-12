package com.pyamsoft.tetherfi.core

import androidx.annotation.CheckResult
import java.util.UUID

@CheckResult
fun generateRandomId(): String {
  return UUID.randomUUID().toString()
}
