package com.pyamsoft.tetherfi.server.urlfixer

import androidx.annotation.CheckResult

interface UrlFixer {

  @CheckResult fun fix(url: String): String
}
