package com.pyamsoft.tetherfi.service.prereq

import androidx.annotation.CheckResult

interface HotspotRequirements {

  @CheckResult suspend fun blockers(): Collection<HotspotStartBlocker>
}
