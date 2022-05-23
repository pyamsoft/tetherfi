package com.pyamsoft.tetherfi.server.proxy.session

import androidx.annotation.CheckResult

interface UrlFixer {

  @CheckResult fun fix(url: String): String
}
