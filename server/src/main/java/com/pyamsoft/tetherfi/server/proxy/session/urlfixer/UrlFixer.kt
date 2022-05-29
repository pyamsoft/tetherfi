package com.pyamsoft.tetherfi.server.proxy.session.urlfixer

import androidx.annotation.CheckResult

interface UrlFixer {

  @CheckResult fun fix(url: String): String
}
