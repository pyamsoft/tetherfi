package com.pyamsoft.widefi.server.proxy.session

import androidx.annotation.CheckResult

interface UrlFixer {

  @CheckResult fun fix(url: String): String
}
