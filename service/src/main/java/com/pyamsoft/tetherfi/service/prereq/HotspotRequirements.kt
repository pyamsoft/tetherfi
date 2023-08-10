package com.pyamsoft.tetherfi.service.prereq

import androidx.annotation.CheckResult

interface HotspotRequirements {

  @CheckResult fun blockers(): List<HotspotStartBlocker>
}
